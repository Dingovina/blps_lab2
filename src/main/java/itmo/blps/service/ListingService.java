package itmo.blps.service;

import itmo.blps.dto.ListingCreateRequest;
import itmo.blps.entity.*;
import itmo.blps.exception.BadRequestException;
import itmo.blps.exception.ForbiddenException;
import itmo.blps.exception.ResourceNotFoundException;
import itmo.blps.repository.ListingRepository;
import itmo.blps.specification.ListingSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ListingService {

    private static final int PUBLICATION_DAYS = 30;

    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public ListingService(ListingRepository listingRepository, NotificationService notificationService) {
        this.listingRepository = listingRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Listing create(User seller, ListingCreateRequest request) {
        Listing l = new Listing();
        l.setSeller(seller);
        mapRequestToListing(request, l);
        l.setStatus(ListingStatus.DRAFT);
        l.setPromotion(PromotionType.NONE);
        return listingRepository.save(l);
    }

    @Transactional
    public Listing update(Long id, User currentUser, ListingCreateRequest request) {
        Listing l = getListingOwnedBy(id, currentUser);
        if (l.getStatus() != ListingStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT listings can be updated");
        }
        mapRequestToListing(request, l);
        return listingRepository.save(l);
    }

    public Listing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));
    }

    public Listing getListingOwnedBy(Long id, User owner) {
        Listing l = getById(id);
        if (!l.getSeller().getId().equals(owner.getId())) {
            throw new ForbiddenException("Not the owner of this listing");
        }
        return l;
    }

    public Page<Listing> findBySeller(Long sellerId, ListingStatus status, Pageable pageable) {
        if (status != null) {
            return listingRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        }
        return listingRepository.findBySellerId(sellerId, pageable);
    }

    @Transactional
    public Listing publish(Long id, boolean forceReject) {
        Listing l = getById(id);
        if (l.getStatus() != ListingStatus.DRAFT) {
            throw new BadRequestException(
                    "Объявление #" + id + " уже опубликовано или недоступно для публикации " +
                    "(текущий статус: " + l.getStatus().name() + "). " +
                    "Публиковать можно только черновики (DRAFT).");
        }
        if (forceReject) {
            throw new BadRequestException("Объявление не соответствует требованиям площадки и не может быть опубликовано");
        }
        Instant now = Instant.now();
        l.setStatus(ListingStatus.ACTIVE);
        l.setPublishedAt(now);
        l.setExpiresAt(now.plus(PUBLICATION_DAYS, ChronoUnit.DAYS));
        l = listingRepository.save(l);
        notificationService.create(
                l.getSeller(),
                NotificationType.LISTING_PUBLISHED,
                "Объявление размещено",
                "Ваше объявление «" + l.getTitle() + "» размещено. Доступно платное продвижение (Топ/Премиум).",
                RelatedEntityType.LISTING,
                l.getId()
        );
        return l;
    }

    @Transactional
    public Listing confirmRelevance(Long id, User seller, boolean relevant) {
        Listing l = getListingOwnedBy(id, seller);
        if (l.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Listing is not active");
        }
        if (relevant) {
            Instant newExpires = Instant.now().plus(PUBLICATION_DAYS, ChronoUnit.DAYS);
            l.setExpiresAt(newExpires);
            l = listingRepository.save(l);
            notificationService.create(
                    l.getSeller(),
                    NotificationType.PUBLICATION_EXTENDED,
                    "Публикация продлена",
                    "Публикация объявления «" + l.getTitle() + "» продлена.",
                    RelatedEntityType.LISTING,
                    l.getId()
            );
        } else {
            l.setStatus(ListingStatus.ARCHIVED);
            listingRepository.save(l);
        }
        return l;
    }

    @Transactional
    public Listing close(Long id, User seller) {
        Listing l = getListingOwnedBy(id, seller);
        l.setStatus(ListingStatus.CLOSED);
        l.setClosedAt(Instant.now());
        l = listingRepository.save(l);
        notificationService.create(
                l.getSeller(),
                NotificationType.LISTING_CLOSED,
                "Объявление закрыто",
                "Объявление «" + l.getTitle() + "» снято с публикации.",
                RelatedEntityType.LISTING,
                l.getId()
        );
        return l;
    }

    public Page<Listing> search(List<String> filters, Pageable pageable) {
        Specification<Listing> spec = ListingSpecification.fromFilter(filters);
        return listingRepository.findAll(spec, pageable);
    }

    public List<Listing> findActiveWithExpiresAtBefore(Instant threshold) {
        return listingRepository.findByStatusAndExpiresAtBefore(ListingStatus.ACTIVE, threshold);
    }

    @Transactional
    public Listing adminSetStatus(Long id, ListingStatus newStatus) {
        Listing l = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));
        ListingStatus oldStatus = l.getStatus();
        l.setStatus(newStatus);
        if (newStatus == ListingStatus.CLOSED && l.getClosedAt() == null) {
            l.setClosedAt(Instant.now());
        }
        l = listingRepository.save(l);
        if (!oldStatus.equals(newStatus)) {
            notificationService.create(
                    l.getSeller(),
                    NotificationType.LISTING_CLOSED,
                    "Статус объявления изменён",
                    "Администратор изменил статус объявления «" + l.getTitle() + "» на " + newStatus.name() + ".",
                    RelatedEntityType.LISTING,
                    l.getId()
            );
        }
        return l;
    }

    private void mapRequestToListing(ListingCreateRequest request, Listing l) {
        if (request.getTitle() != null) l.setTitle(request.getTitle());
        if (request.getDescription() != null) l.setDescription(request.getDescription());
        if (request.getAddress() != null) l.setAddress(request.getAddress());
        if (request.getRegion() != null) l.setRegion(request.getRegion());
        if (request.getPrice() != null) l.setPrice(request.getPrice());
        if (request.getAreaSqm() != null) l.setAreaSqm(request.getAreaSqm());
        if (request.getRooms() != null) l.setRooms(request.getRooms());
    }
}
