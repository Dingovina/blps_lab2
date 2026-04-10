package itmo.blps.service;

import itmo.blps.entity.*;
import itmo.blps.exception.BadRequestException;
import itmo.blps.exception.ForbiddenException;
import itmo.blps.exception.ResourceNotFoundException;
import itmo.blps.repository.InquiryRepository;
import itmo.blps.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public InquiryService(InquiryRepository inquiryRepository,
                          ListingRepository listingRepository,
                          NotificationService notificationService) {
        this.inquiryRepository = inquiryRepository;
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Inquiry create(User buyer, Long listingId, String message) {
        if (buyer.getRole() != UserRole.BUYER) {
            throw new ForbiddenException("Only buyers can create inquiries");
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Listing is not active");
        }
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new BadRequestException("Cannot create inquiry for your own listing");
        }
        if (inquiryRepository.existsByListingIdAndBuyerId(listingId, buyer.getId())) {
            throw new BadRequestException("You already have an inquiry for this listing");
        }
        Inquiry inquiry = new Inquiry();
        inquiry.setListing(listing);
        inquiry.setBuyer(buyer);
        inquiry.setMessage(message);
        inquiry.setStatus(InquiryStatus.PENDING);
        inquiry = inquiryRepository.save(inquiry);
        User seller = listing.getSeller();
        notificationService.create(
                seller,
                NotificationType.NEW_INQUIRY,
                "Новое обращение",
                "Покупатель оставил запрос на показ объявления «" + listing.getTitle() + "».",
                RelatedEntityType.INQUIRY,
                inquiry.getId()
        );
        return inquiry;
    }

    public Inquiry getById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", id));
    }

    public Inquiry getInquiryOwnedByBuyer(Long inquiryId, User user) {
        Inquiry i = getById(inquiryId);
        if (!i.getBuyer().getId().equals(user.getId())) {
            throw new ForbiddenException("Not your inquiry");
        }
        return i;
    }

    public Inquiry getInquiryOwnedBySeller(Long inquiryId, User user) {
        Inquiry i = getById(inquiryId);
        if (!i.getListing().getSeller().getId().equals(user.getId())) {
            throw new ForbiddenException("Not the seller of this listing");
        }
        return i;
    }

    public Page<Inquiry> findByBuyer(Long buyerId, InquiryStatus status, Pageable pageable) {
        if (status != null) {
            return inquiryRepository.findByBuyerIdAndStatus(buyerId, status, pageable);
        }
        return inquiryRepository.findByBuyerId(buyerId, pageable);
    }

    public Page<Inquiry> findBySeller(Long sellerId, InquiryStatus status, Pageable pageable) {
        if (status != null) {
            return inquiryRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        }
        return inquiryRepository.findBySellerId(sellerId, pageable);
    }

    public Page<Inquiry> findByListingIdAndBuyer(Long listingId, Long buyerId, InquiryStatus status, Pageable pageable) {
        return inquiryRepository.findByListingIdAndBuyerId(listingId, buyerId, pageable);
    }

    public Page<Inquiry> findByListingIdAndSeller(Long listingId, Long sellerId, InquiryStatus status, Pageable pageable) {
        return inquiryRepository.findByListingIdAndSellerId(listingId, sellerId, pageable);
    }

    @Transactional
    public Inquiry resolveShowing(Long inquiryId, User seller,
                                  itmo.blps.dto.ShowingDecisionRequest.Decision decision,
                                  Instant scheduledAt, String contactInfo, String reason) {
        if (decision == itmo.blps.dto.ShowingDecisionRequest.Decision.CONFIRM) {
            return confirmShowing(inquiryId, seller, scheduledAt, contactInfo);
        } else {
            return rejectShowing(inquiryId, seller, reason);
        }
    }

    @Transactional
    public Inquiry confirmShowing(Long inquiryId, User seller, Instant scheduledAt, String contactInfo) {
        Inquiry i = getInquiryOwnedBySeller(inquiryId, seller);
        if (i.getStatus() != InquiryStatus.PENDING) {
            throw new BadRequestException("Inquiry is not pending");
        }
        i.setStatus(InquiryStatus.SHOWING_SCHEDULED);
        i.setScheduledAt(scheduledAt);
        i.setContactInfo(contactInfo);
        i = inquiryRepository.save(i);
        User buyer = i.getBuyer();
        notificationService.create(
                buyer,
                NotificationType.SHOWING_SCHEDULED,
                "Показ назначен",
                "Продавец назначил показ по объявлению «" + i.getListing().getTitle() + "». Время: " + (scheduledAt != null ? scheduledAt : "уточняйте") + ". Контакт: " + (contactInfo != null ? contactInfo : "—"),
                RelatedEntityType.INQUIRY,
                i.getId()
        );
        return i;
    }

    @Transactional
    public Inquiry rejectShowing(Long inquiryId, User seller, String reason) {
        Inquiry i = getInquiryOwnedBySeller(inquiryId, seller);
        if (i.getStatus() != InquiryStatus.PENDING) {
            throw new BadRequestException("Inquiry is not pending");
        }
        i.setStatus(InquiryStatus.SHOWING_REJECTED);
        i.setRejectReason(reason);
        i = inquiryRepository.save(i);
        User buyer = i.getBuyer();
        notificationService.create(
                buyer,
                NotificationType.SHOWING_REJECTED,
                "Отказ в показе",
                reason != null && !reason.isBlank() ? reason : "Продавец отказал в показе по объявлению «" + i.getListing().getTitle() + "».",
                RelatedEntityType.INQUIRY,
                i.getId()
        );
        return i;
    }

    @Transactional
    public Inquiry visitResult(Long inquiryId, User buyer, Boolean willBuy) {
        Inquiry i = getInquiryOwnedByBuyer(inquiryId, buyer);
        if (i.getStatus() != InquiryStatus.SHOWING_SCHEDULED) {
            throw new BadRequestException("Inquiry must be in SHOWING_SCHEDULED to submit visit result");
        }
        i.setStatus(InquiryStatus.COMPLETED);
        i.setWillBuy(Boolean.TRUE.equals(willBuy));
        i = inquiryRepository.save(i);
        if (Boolean.TRUE.equals(willBuy)) {
            User seller = i.getListing().getSeller();
            notificationService.create(
                    seller,
                    NotificationType.LISTING_CLOSED,
                    "Рекомендуется закрыть объявление",
                    "Покупатель по обращению к объявлению «" + i.getListing().getTitle() + "» принял решение о покупке. Закройте объявление.",
                    RelatedEntityType.LISTING,
                    i.getListing().getId()
            );
        }
        return i;
    }
}
