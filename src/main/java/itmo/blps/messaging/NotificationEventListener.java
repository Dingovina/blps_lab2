package itmo.blps.messaging;

import itmo.blps.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailNotificationService emailNotificationService;

    public NotificationEventListener(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @JmsListener(destination = "${app.messaging.notifications-queue}",
            containerFactory = "jmsListenerContainerFactory")
    public void receive(NotificationEvent event) {
        log.info("Received notification email event {} for notification {}",
                event.getEventId(), event.getNotificationId());
        emailNotificationService.send(event);
    }
}
