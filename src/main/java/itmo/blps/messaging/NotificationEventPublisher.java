package itmo.blps.messaging;

import itmo.blps.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final MessagingProperties messagingProperties;

    public NotificationEventPublisher(JmsTemplate jmsTemplate, MessagingProperties messagingProperties) {
        this.jmsTemplate = jmsTemplate;
        this.messagingProperties = messagingProperties;
    }

    public void publish(NotificationEvent event) {
        jmsTemplate.convertAndSend(messagingProperties.getNotificationsQueue(), event);
        log.info("Published notification email event {} for notification {}",
                event.getEventId(), event.getNotificationId());
    }
}
