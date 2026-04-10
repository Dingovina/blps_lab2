package itmo.blps.repository;

import itmo.blps.entity.Notification;
import itmo.blps.entity.NotificationType;
import itmo.blps.entity.RelatedEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUser_Id(Long userId, Pageable pageable);

    Page<Notification> findByUser_IdAndRead(Long userId, boolean read, Pageable pageable);

    boolean existsByUser_IdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
            Long userId, NotificationType type, RelatedEntityType relatedEntityType, Long relatedEntityId);
}
