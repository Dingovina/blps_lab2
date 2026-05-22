package itmo.blps.dto;

import itmo.blps.entity.Notification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String body;
    private String relatedEntityType;
    private Long relatedEntityId;
    private boolean read;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getType().name());
        r.setTitle(n.getTitle());
        r.setBody(n.getBody());
        r.setRelatedEntityType(n.getRelatedEntityType() != null ? n.getRelatedEntityType().name() : null);
        r.setRelatedEntityId(n.getRelatedEntityId());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}
