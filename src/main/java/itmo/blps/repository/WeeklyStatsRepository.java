package itmo.blps.repository;

import itmo.blps.entity.WeeklyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface WeeklyStatsRepository extends JpaRepository<WeeklyStats, Long> {

    Optional<WeeklyStats> findTopByUser_IdOrderByPeriodStartDesc(Long userId);

    boolean existsByUser_IdAndPeriodStart(Long userId, Instant periodStart);
}
