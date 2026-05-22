package itmo.blps.service;

import itmo.blps.entity.*;
import itmo.blps.exception.ResourceNotFoundException;
import itmo.blps.repository.InquiryRepository;
import itmo.blps.repository.ListingRepository;
import itmo.blps.repository.UserRepository;
import itmo.blps.repository.WeeklyStatsRepository;
import itmo.blps.util.WeeklyPeriodUtil;
import itmo.blps.util.WeeklyPeriodUtil.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WeeklyStatsService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneOffset.UTC);

    private final WeeklyStatsRepository weeklyStatsRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final InquiryRepository inquiryRepository;
    private final NotificationService notificationService;

    public WeeklyStatsService(WeeklyStatsRepository weeklyStatsRepository,
                              UserRepository userRepository,
                              ListingRepository listingRepository,
                              InquiryRepository inquiryRepository,
                              NotificationService notificationService) {
        this.weeklyStatsRepository = weeklyStatsRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.inquiryRepository = inquiryRepository;
        this.notificationService = notificationService;
    }

    public WeeklyStats getLatestForUser(Long userId) {
        return weeklyStatsRepository.findTopByUser_IdOrderByPeriodStartDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Weekly statistics not found for user id: " + userId));
    }

    @Transactional
    public void generateWeeklyReports() {
        Period period = WeeklyPeriodUtil.previousCalendarWeek(Instant.now());
        List<User> sellers = userRepository.findByRole(UserRole.SELLER);
        List<User> buyers = userRepository.findByRole(UserRole.BUYER);

        for (User seller : sellers) {
            generateIfAbsent(seller, period);
        }
        for (User buyer : buyers) {
            generateIfAbsent(buyer, period);
        }
    }

    private void generateIfAbsent(User user, Period period) {
        if (weeklyStatsRepository.existsByUser_IdAndPeriodStart(user.getId(), period.start())) {
            return;
        }
        WeeklyStats stats = buildReport(user, period);
        stats = weeklyStatsRepository.save(stats);
        notificationService.create(
                user,
                NotificationType.WEEKLY_STATS,
                "Еженедельная статистика",
                formatNotificationBody(stats),
                null,
                null
        );
    }

    private WeeklyStats buildReport(User user, Period period) {
        WeeklyStats stats = new WeeklyStats();
        stats.setUser(user);
        stats.setRole(user.getRole());
        stats.setPeriodStart(period.start());
        stats.setPeriodEnd(period.end());

        Instant start = period.start();
        Instant end = period.end();

        if (user.getRole() == UserRole.SELLER) {
            long published = listingRepository.countPublishedBySellerIdAndPublishedAtBetween(
                    user.getId(), start, end);
            long closed = listingRepository.countClosedBySellerIdAndClosedAtBetween(
                    user.getId(), start, end);
            long completed = inquiryRepository.countBySellerIdAndStatusAndUpdatedAtBetween(
                    user.getId(), InquiryStatus.COMPLETED, start, end);
            long scheduled = inquiryRepository.countBySellerIdAndStatusAndUpdatedAtBetween(
                    user.getId(), InquiryStatus.SHOWING_SCHEDULED, start, end);
            stats.setPublishedListings((int) published);
            stats.setClosedListings((int) closed);
            stats.setCompletedInquiries((int) completed);
            stats.setScheduledInquiries((int) scheduled);
        } else if (user.getRole() == UserRole.BUYER) {
            long requests = inquiryRepository.countByBuyerIdAndCreatedAtBetween(
                    user.getId(), start, end);
            long scheduled = inquiryRepository.countByBuyerIdAndStatusAndUpdatedAtBetween(
                    user.getId(), InquiryStatus.SHOWING_SCHEDULED, start, end);
            long rejected = inquiryRepository.countByBuyerIdAndStatusAndUpdatedAtBetween(
                    user.getId(), InquiryStatus.SHOWING_REJECTED, start, end);
            long completed = inquiryRepository.countByBuyerIdAndStatusAndUpdatedAtBetween(
                    user.getId(), InquiryStatus.COMPLETED, start, end);
            stats.setShowRequests((int) requests);
            stats.setScheduledShowings((int) scheduled);
            stats.setRejectedShowings((int) rejected);
            stats.setCompletedShowings((int) completed);
        }

        return stats;
    }

    private String formatNotificationBody(WeeklyStats stats) {
        String range = DATE_FMT.format(stats.getPeriodStart()) + " – " + DATE_FMT.format(stats.getPeriodEnd());
        if (stats.getRole() == UserRole.SELLER) {
            return "Сводка за " + range + ": опубликовано " + stats.getPublishedListings()
                    + ", закрыто " + stats.getClosedListings()
                    + ", завершённых обращений " + stats.getCompletedInquiries()
                    + ", назначенных показов " + stats.getScheduledInquiries()
                    + ". Подробности: GET /api/weekly";
        }
        return "Сводка за " + range + ": запросов на показ " + stats.getShowRequests()
                + ", запланировано " + stats.getScheduledShowings()
                + ", отклонено " + stats.getRejectedShowings()
                + ", завершено " + stats.getCompletedShowings()
                + ". Подробности: GET /api/weekly";
    }
}
