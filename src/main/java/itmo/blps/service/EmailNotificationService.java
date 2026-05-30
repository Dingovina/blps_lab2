package itmo.blps.service;

import itmo.blps.config.MailProperties;
import itmo.blps.messaging.NotificationEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final NotificationEmailRenderer emailRenderer;

    public EmailNotificationService(JavaMailSender mailSender,
                                    MailProperties mailProperties,
                                    NotificationEmailRenderer emailRenderer) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.emailRenderer = emailRenderer;
    }

    public void send(NotificationEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(event.getUserEmail());
            helper.setSubject(formatSubject(event.getTitle()));
            helper.setText(emailRenderer.renderPlainText(event), emailRenderer.renderHtml(event));

            mailSender.send(message);
            log.info("Sent email for notification {} event {} to {}",
                    event.getNotificationId(), event.getEventId(), event.getUserEmail());
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to send notification email for event " + event.getEventId(), ex);
        }
    }

    private String formatSubject(String title) {
        return mailProperties.getBrandName() + " · " + title;
    }
}
