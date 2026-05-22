package itmo.blps.messaging;

import itmo.blps.entity.NotificationType;
import itmo.blps.entity.RelatedEntityType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationEvent {

    private String eventId;
    private Long notificationId;
    private Long userId;
    private String userEmail;
    private NotificationType type;
    private String title;
    private String body;
    private RelatedEntityType relatedEntityType;
    private Long relatedEntityId;
}
