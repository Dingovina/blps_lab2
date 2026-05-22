package itmo.blps.service;

import itmo.blps.config.MailProperties;
import itmo.blps.messaging.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public EmailNotificationService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    public void send(NotificationEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(event.getUserEmail());
        message.setSubject(event.getTitle());
        message.setText(event.getBody());

        mailSender.send(message);
        log.info("Sent email for notification {} event {} to {}",
                event.getNotificationId(), event.getEventId(), event.getUserEmail());
    }
}
