package itmo.blps.config;

import itmo.blps.service.WeeklyStatsService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!worker")
@Order(2)
public class WeeklyStatsStartupRunner implements ApplicationRunner {

    private final WeeklyStatsService weeklyStatsService;

    public WeeklyStatsStartupRunner(WeeklyStatsService weeklyStatsService) {
        this.weeklyStatsService = weeklyStatsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        weeklyStatsService.generateWeeklyReports();
    }
}
