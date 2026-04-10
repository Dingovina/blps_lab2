package itmo.blps.service;

import itmo.blps.entity.Notification;
import itmo.blps.entity.NotificationType;
import itmo.blps.entity.RelatedEntityType;
import itmo.blps.entity.User;
import itmo.blps.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
        return notificationRepository.save(n);
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
}
