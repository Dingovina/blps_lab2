package itmo.blps.controller;

import itmo.blps.dto.WeeklyStatsResponse;
import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import itmo.blps.entity.WeeklyStats;
import itmo.blps.exception.ForbiddenException;
import itmo.blps.security.AuthUtil;
import itmo.blps.service.WeeklyStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeeklyStatsController {

    private final WeeklyStatsService weeklyStatsService;

    public WeeklyStatsController(WeeklyStatsService weeklyStatsService) {
        this.weeklyStatsService = weeklyStatsService;
    }

    @PreAuthorize("hasAuthority('PRIV_BASIC_ACCESS')")
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStatsResponse> getWeeklyStats() {
        User user = AuthUtil.currentUser();
        if (user.getRole() == UserRole.ADMIN) {
            throw new ForbiddenException("Weekly statistics are not available for administrators");
        }
        WeeklyStats stats = weeklyStatsService.getLatestForUser(user.getId());
        return ResponseEntity.ok(WeeklyStatsResponse.from(stats));
    }
}
