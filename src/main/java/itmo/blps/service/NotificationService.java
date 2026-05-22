package itmo.blps.service;

import itmo.blps.entity.Notification;
import itmo.blps.entity.NotificationType;
import itmo.blps.entity.RelatedEntityType;
import itmo.blps.entity.User;
import itmo.blps.messaging.NotificationEvent;
import itmo.blps.messaging.NotificationEventPublisher;
import itmo.blps.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationEventPublisher notificationEventPublisher) {
        this.notificationRepository = notificationRepository;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional
    public Notification create(User user, NotificationType type, String title, String body,
                              RelatedEntityType relatedEntityType, Long relatedEntityId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setRelatedEntityType(relatedEntityType);
        n.setRelatedEntityId(relatedEntityId);
        n = notificationRepository.save(n);
        publishEmailEventAfterCommit(toEvent(n));
        return n;
    }

    public Page<Notification> findByUserId(Long userId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUser_IdAndRead(userId, false, pageable);
        }
        return notificationRepository.findByUser_Id(userId, pageable);
    }

    @Transactional
    public Notification markRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new itmo.blps.exception.ResourceNotFoundException("Notification", id));
        if (!n.getUser().getId().equals(userId)) {
            throw new itmo.blps.exception.ForbiddenException("Not your notification");
        }
        n.setRead(true);
        return notificationRepository.save(n);
    }

    private NotificationEvent toEvent(Notification notification) {
        NotificationEvent event = new NotificationEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setNotificationId(notification.getId());
        event.setUserId(notification.getUser().getId());
        event.setUserEmail(notification.getUser().getEmail());
        event.setType(notification.getType());
        event.setTitle(notification.getTitle());
        event.setBody(notification.getBody());
        event.setRelatedEntityType(notification.getRelatedEntityType());
        event.setRelatedEntityId(notification.getRelatedEntityId());
        return event;
    }

    private void publishEmailEventAfterCommit(NotificationEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishEmailEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishEmailEvent(event);
            }
        });
    }

    private void publishEmailEvent(NotificationEvent event) {
        try {
            notificationEventPublisher.publish(event);
        } catch (RuntimeException ex) {
            log.error("Failed to publish notification email event {} for notification {}",
                    event.getEventId(), event.getNotificationId(), ex);
        }
    }
}
