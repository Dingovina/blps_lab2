package itmo.blps.service;

import itmo.blps.entity.*;
import itmo.blps.exception.BadRequestException;
import itmo.blps.exception.ForbiddenException;
import itmo.blps.exception.ResourceNotFoundException;
import itmo.blps.integration.crm.CrmSyncHelper;
import itmo.blps.integration.crm.CrmSyncService;
import itmo.blps.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import itmo.blps.repository.PaymentRepository;
import itmo.blps.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final double SUCCESS_PROBABILITY = 0.7;

    private final PaymentRepository paymentRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CrmSyncHelper crmSyncHelper;
    private final CrmSyncService crmSyncService;

    public PaymentService(PaymentRepository paymentRepository,
                          ListingRepository listingRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          CrmSyncHelper crmSyncHelper,
                          @Autowired(required = false) CrmSyncService crmSyncService) {
        this.paymentRepository = paymentRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.crmSyncHelper = crmSyncHelper;
        this.crmSyncService = crmSyncService;
    }

    @Transactional
    public PaymentResult pay(Long listingId, User user, PromotionType promotionType) {
        if (promotionType == PromotionType.NONE) {
            throw new BadRequestException("Promotion type must be TOP or PREMIUM");
        }
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", user.getId()));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
        if (!listing.getSeller().getId().equals(managedUser.getId())) {
            throw new ForbiddenException("Not the owner of this listing");
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Listing must be active to pay for promotion");
        }

        Payment payment = new Payment();
        payment.setListing(listing);
        payment.setUser(managedUser);
        payment.setPromotionType(promotionType);
        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        boolean success = ThreadLocalRandom.current().nextDouble() < SUCCESS_PROBABILITY;
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            listing.setPromotion(promotionType);
            listingRepository.save(listing);
            paymentRepository.save(payment);
            notificationService.create(
                    managedUser,
                    NotificationType.PROMOTION_ACTIVATED,
                    "Продвижение активировано",
                    "Услуга продвижения («" + promotionType.name() + "») для объявления «" + listing.getTitle() + "» успешно активирована.",
                    RelatedEntityType.PAYMENT,
                    payment.getId()
            );
            final Listing promotedListing = listing;
            crmSyncHelper.afterCommit(() -> crmSyncService.syncListingPromotion(promotedListing), crmSyncService);
            return new PaymentResult(payment.getId(), PaymentStatus.SUCCESS, "Оплата прошла успешно");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            notificationService.create(
                    managedUser,
                    NotificationType.PROMOTION_PAYMENT_FAILED,
                    "Ошибка оплаты",
                    "Не удалось провести оплату продвижения для объявления «" + listing.getTitle() + "». Объявление опубликовано без продвижения.",
                    RelatedEntityType.PAYMENT,
                    payment.getId()
            );
            return new PaymentResult(payment.getId(), PaymentStatus.FAILED, "Оплата не прошла");
        }
    }

    public record PaymentResult(long paymentId, PaymentStatus status, String message) {}
}
