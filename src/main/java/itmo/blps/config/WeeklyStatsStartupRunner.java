package itmo.blps.config;

import itmo.blps.service.WeeklyStatsService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!worker")
public class WeeklyStatsStartupRunner {

    private final WeeklyStatsService weeklyStatsService;

    public WeeklyStatsStartupRunner(WeeklyStatsService weeklyStatsService) {
        this.weeklyStatsService = weeklyStatsService;
    }

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        weeklyStatsService.generateWeeklyReports();
    }
}
