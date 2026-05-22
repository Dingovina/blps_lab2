package itmo.blps.config;

import itmo.blps.service.WeeklyStatsService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!worker")
public class WeeklyStatsScheduler {

    private final WeeklyStatsService weeklyStatsService;

    public WeeklyStatsScheduler(WeeklyStatsService weeklyStatsService) {
        this.weeklyStatsService = weeklyStatsService;
    }

    @Scheduled(cron = "${app.weekly-stats.cron:0 0 9 * * MON}")
    @Transactional
    public void generatePreviousWeekReports() {
        weeklyStatsService.generateWeeklyReports();
    }
}
