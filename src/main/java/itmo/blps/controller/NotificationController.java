package itmo.blps.controller;

import itmo.blps.dto.NotificationResponse;
import itmo.blps.dto.PageResponse;
import itmo.blps.entity.Notification;
import itmo.blps.entity.User;
import itmo.blps.security.AuthUtil;
import itmo.blps.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_NOTIFICATIONS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @GetMapping("/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = AuthUtil.currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = notificationService.findByUserId(user.getId(), unreadOnly, pageable);
        List<NotificationResponse> content = p.getContent().stream().map(NotificationResponse::from).collect(Collectors.toList());
        PageResponse<NotificationResponse> resp = new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_NOTIFICATIONS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        User user = AuthUtil.currentUser();
        Notification n = notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(NotificationResponse.from(n));
    }
}
