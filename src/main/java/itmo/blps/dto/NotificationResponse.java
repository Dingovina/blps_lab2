package itmo.blps.dto;

import itmo.blps.entity.Notification;

import java.time.Instant;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
