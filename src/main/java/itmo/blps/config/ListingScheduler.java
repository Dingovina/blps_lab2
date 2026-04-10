package itmo.blps.config;

import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.NotificationType;
import itmo.blps.entity.RelatedEntityType;
import itmo.blps.repository.ListingRepository;
import itmo.blps.repository.NotificationRepository;
import itmo.blps.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ListingScheduler {

    private final ListingRepository listingRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Value("${app.listing.archive-grace-days:7}")
    private int archiveGraceDays;

    public ListingScheduler(ListingRepository listingRepository,
                            NotificationRepository notificationRepository,
                            NotificationService notificationService) {
        this.listingRepository = listingRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    /** Notify sellers about listings that have reached expires_at (once per listing). Runs daily at 2:00. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void sendArchivationSoonNotifications() {
        Instant now = Instant.now();
        List<Listing> expired = listingRepository.findByStatusAndExpiresAtBefore(ListingStatus.ACTIVE, now);
        for (Listing l : expired) {
            Long sellerId = l.getSeller().getId();
            if (!notificationRepository.existsByUser_IdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                    sellerId, NotificationType.ARCHIVATION_SOON, RelatedEntityType.LISTING, l.getId())) {
                notificationService.create(
                        l.getSeller(),
                        NotificationType.ARCHIVATION_SOON,
                        "Срок размещения истекает",
                        "Срок размещения объявления «" + l.getTitle() + "» истёк. Подтвердите актуальность или объявление будет архивировано.",
                        RelatedEntityType.LISTING,
                        l.getId()
                );
            }
        }
    }

    /** Auto-archive listings that have been expired for more than archive-grace-days. Runs daily at 3:00. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void autoArchiveExpiredListings() {
        Instant threshold = Instant.now().minus(archiveGraceDays, ChronoUnit.DAYS);
        List<Listing> toArchive = listingRepository.findByStatusAndExpiresAtBefore(ListingStatus.ACTIVE, threshold);
        for (Listing l : toArchive) {
            l.setStatus(ListingStatus.ARCHIVED);
            listingRepository.save(l);
        }
    }
}
