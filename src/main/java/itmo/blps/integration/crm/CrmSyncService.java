package itmo.blps.integration.crm;

import itmo.blps.bitrix.jca.BitrixConnection;
import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.BitrixDealErrors;
import itmo.blps.bitrix.jca.BitrixEventRecord;
import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;
import itmo.blps.config.BitrixProperties;
import itmo.blps.dto.ListingCreateRequest;
import itmo.blps.entity.CrmLink;
import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.PromotionType;
import itmo.blps.entity.User;
import itmo.blps.repository.CrmLinkRepository;
import itmo.blps.repository.ListingRepository;
import jakarta.resource.ResourceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class CrmSyncService {

    private static final Logger log = LoggerFactory.getLogger(CrmSyncService.class);

    private final BitrixConnectionFactoryInterface connectionFactory;
    private final BitrixProperties properties;
    private final CrmLinkRepository crmLinkRepository;
    private final ListingRepository listingRepository;
    private final Validator validator;

    public CrmSyncService(BitrixConnectionFactoryInterface connectionFactory,
                          BitrixProperties properties,
                          CrmLinkRepository crmLinkRepository,
                          ListingRepository listingRepository,
                          Validator validator) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.crmLinkRepository = crmLinkRepository;
        this.listingRepository = listingRepository;
        this.validator = validator;
    }

    @Transactional
    public void syncListingPublished(Listing listing) {
        if (BitrixSyncContext.isInbound() || !properties.isEnabled()) {
            return;
        }
        listingRepository.findById(listing.getId()).ifPresent(managed -> runWithConnection(conn -> {
            int contactId = ensureContact(conn, managed.getSeller());
            Map<String, Object> fields = dealFields(managed);
            fields.put("STAGE_ID", stageForStatus(managed.getStatus()));
            fields.put("CONTACT_ID", contactId);
            fields.put("CATEGORY_ID", properties.getDealCategoryId());

            Optional<CrmLink> existing = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, managed.getId());
            if (existing.isPresent()) {
                updateLinkedDeal(conn, existing.get(), managed, fields);
            } else {
                int dealId = conn.createDeal(fields);
                saveLink(CrmEntityType.LISTING, managed.getId(), dealId);
            }
        }));
    }

    @Transactional
    public void syncListingStatus(Listing listing) {
        if (BitrixSyncContext.isInbound() || !properties.isEnabled()) {
            return;
        }
        listingRepository.findById(listing.getId()).ifPresent(managed -> runWithConnection(conn -> {
            upsertDealForListing(conn, managed);
        }));
    }


    @Transactional
    public void backfillExistingData() {
        if (!properties.isEnabled()) {
            return;
        }
        int listings = 0;
        for (Listing listing : listingRepository.findAll()) {
            try {
                syncListingStatus(listing);
                listings++;
            } catch (RuntimeException e) {
                log.warn("Bitrix backfill failed for listing {}: {}", listing.getId(), e.getMessage());
            }
        }
        log.info("Bitrix backfill finished: {} listings processed", listings);
    }

    @Transactional
    public void reconcileDeletedDeals() {
        if (!properties.isEnabled()) {
            return;
        }
        List<Long> listingIdsToDelete = new ArrayList<>();
        runWithConnection(conn -> {
            for (CrmLink link : crmLinkRepository.findAllByEntityType(CrmEntityType.LISTING)) {
                if (conn.getDeal(link.getBitrixId()).isEmpty()) {
                    listingIdsToDelete.add(link.getLocalId());
                }
            }
        });
        for (Long listingId : listingIdsToDelete) {
            deleteListingByBitrixDeletion(listingId);
        }
    }

    @Transactional
    public void deleteListingByBitrixDeletion(long listingId) {
        BitrixSyncContext.runInbound(() -> {
            crmLinkRepository.deleteByEntityTypeAndLocalId(CrmEntityType.LISTING, listingId);
            if (listingRepository.existsById(listingId)) {
                listingRepository.deleteById(listingId);
                log.info("Listing {} deleted because Bitrix deal was removed", listingId);
            }
        });
    }

    @Transactional
    public void syncListingPromotion(Listing listing) {
        if (BitrixSyncContext.isInbound() || !properties.isEnabled()) {
            return;
        }
        listingRepository.findById(listing.getId()).ifPresent(managed -> runWithConnection(conn -> {
            Optional<CrmLink> link = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, managed.getId());
            if (link.isEmpty()) {
                return;
            }
            Map<String, Object> fields = new HashMap<>();
            fields.put(properties.getDealFieldPromotion(), promotionLabel(managed.getPromotion()));
            updateLinkedDeal(conn, link.get(), managed, fields);
        }));
    }

    @Transactional
    public void applyInboundDeal(BitrixEventRecord event) {
        BitrixDealSnapshot deal = event.getDeal();
        Object listingIdRaw = deal.getField(properties.getDealFieldListingId());
        if (listingIdRaw == null) {
            return;
        }
        long listingId = toLong(listingIdRaw);
        Optional<CrmLink> link = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, listingId);
        if (link.isEmpty()) {
            saveLink(CrmEntityType.LISTING, listingId, deal.getId());
        } else if (!link.get().getBitrixId().equals(deal.getId())) {
            log.warn("Bitrix deal {} does not match stored link for listing {}", deal.getId(), listingId);
        }

        listingRepository.findById(listingId).ifPresent(listing -> {
            ListingCreateRequest current = listingToRequest(listing);
            ListingCreateRequest prospective = listingToRequest(listing);
            overlayInboundListingFields(prospective, listing, deal);

            if (listingFieldsDiffer(current, prospective)) {
                Optional<String> validationError = validateListingCreateRequest(prospective);
                if (validationError.isPresent()) {
                    log.warn("Inbound Bitrix deal {} rejected for listing {}: {}",
                            deal.getId(), listingId, validationError.get());
                    revertDealInBitrix(listing);
                    return;
                }
            }

            BitrixSyncContext.runInbound(() -> {
                boolean statusChanged = applyInboundStatus(listing, deal);
                boolean titleChanged = applyInboundTitle(listing, deal);
                boolean priceChanged = applyInboundPrice(listing, deal);
                boolean descriptionChanged = applyInboundDescription(listing, deal);
                boolean addressChanged = applyInboundAddress(listing, deal);
                boolean areaChanged = applyInboundArea(listing, deal);
                boolean roomsChanged = applyInboundRooms(listing, deal);
                if (statusChanged || titleChanged || priceChanged || descriptionChanged
                        || addressChanged || areaChanged || roomsChanged) {
                    listingRepository.save(listing);
                    if (statusChanged) {
                        log.info("Inbound Bitrix: listing {} status -> {}", listingId, listing.getStatus());
                    }
                    if (titleChanged) {
                        log.info("Inbound Bitrix: listing {} title -> {}", listingId, listing.getTitle());
                    }
                    if (priceChanged) {
                        log.info("Inbound Bitrix: listing {} price -> {}", listingId, listing.getPrice());
                    }
                    if (descriptionChanged) {
                        log.info("Inbound Bitrix: listing {} description updated", listingId);
                    }
                    if (addressChanged) {
                        log.info("Inbound Bitrix: listing {} address -> {}", listingId, listing.getAddress());
                    }
                    if (areaChanged) {
                        log.info("Inbound Bitrix: listing {} area -> {}", listingId, listing.getAreaSqm());
                    }
                    if (roomsChanged) {
                        log.info("Inbound Bitrix: listing {} rooms -> {}", listingId, listing.getRooms());
                    }
                }
            });
            crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, listingId)
                    .ifPresent(this::touchLink);
        });
    }

    private static ListingCreateRequest listingToRequest(Listing listing) {
        ListingCreateRequest request = new ListingCreateRequest();
        request.setTitle(listing.getTitle());
        request.setDescription(listing.getDescription());
        request.setAddress(listing.getAddress());
        request.setRegion(listing.getRegion());
        request.setPrice(listing.getPrice());
        request.setAreaSqm(listing.getAreaSqm());
        request.setRooms(listing.getRooms());
        return request;
    }

    private void overlayInboundListingFields(ListingCreateRequest request, Listing listing, BitrixDealSnapshot deal) {
        if (deal.getFields() != null && deal.getFields().containsKey("TITLE")) {
            String incoming = normalizeInboundTitle(deal.getField("TITLE"));
            if (!Objects.equals(listing.getTitle(), incoming)) {
                request.setTitle(incoming);
            }
        }
        if (deal.getFields() != null && deal.getFields().containsKey("OPPORTUNITY")) {
            BigDecimal incoming = toBigDecimal(deal.getField("OPPORTUNITY"));
            if (incoming != null && listing.getPrice().compareTo(incoming) != 0) {
                request.setPrice(incoming);
            }
        }
        if (deal.getFields() != null && deal.getFields().containsKey("COMMENTS")) {
            Object raw = deal.getField("COMMENTS");
            String normalized = raw == null || raw.toString().isBlank() ? null : raw.toString();
            if (!Objects.equals(listing.getDescription(), normalized)) {
                request.setDescription(normalized);
            }
        }
        String addressField = properties.getDealFieldAddress();
        if (addressField != null && deal.getFields() != null && deal.getFields().containsKey(addressField)) {
            Object raw = deal.getField(addressField);
            String incoming = raw == null || raw.toString().isBlank() ? null : raw.toString();
            if (!Objects.equals(listing.getAddress(), incoming)) {
                request.setAddress(incoming);
            }
        }
        String areaField = properties.getDealFieldArea();
        if (areaField != null && deal.getFields() != null && deal.getFields().containsKey(areaField)) {
            BigDecimal incoming = toBigDecimal(deal.getField(areaField));
            if (incoming != null
                    && (listing.getAreaSqm() == null || listing.getAreaSqm().compareTo(incoming) != 0)) {
                request.setAreaSqm(incoming);
            }
        }
        String roomsField = properties.getDealFieldRooms();
        if (roomsField != null && deal.getFields() != null && deal.getFields().containsKey(roomsField)) {
            BigDecimal incoming = toBigDecimal(deal.getField(roomsField));
            if (incoming != null) {
                int rooms = incoming.intValue();
                if (listing.getRooms() == null || listing.getRooms() != rooms) {
                    request.setRooms(rooms);
                }
            }
        }
    }

    private static boolean listingFieldsDiffer(ListingCreateRequest current, ListingCreateRequest prospective) {
        return !Objects.equals(current.getTitle(), prospective.getTitle())
                || !Objects.equals(current.getPrice(), prospective.getPrice())
                || !Objects.equals(current.getDescription(), prospective.getDescription())
                || !Objects.equals(current.getAddress(), prospective.getAddress())
                || !Objects.equals(current.getAreaSqm(), prospective.getAreaSqm())
                || !Objects.equals(current.getRooms(), prospective.getRooms());
    }

    private Optional<String> validateListingCreateRequest(ListingCreateRequest request) {
        Set<ConstraintViolation<ListingCreateRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; ")));
    }

    private void revertDealInBitrix(Listing listing) {
        runWithConnection(conn -> {
            Optional<CrmLink> link = crmLinkRepository.findByEntityTypeAndLocalId(
                    CrmEntityType.LISTING, listing.getId());
            if (link.isEmpty()) {
                log.warn("Cannot revert Bitrix deal: no CRM link for listing {}", listing.getId());
                return;
            }
            upsertDealForListing(conn, listing);
            log.info("Reverted Bitrix deal {} to DB state for listing {}",
                    link.get().getBitrixId(), listing.getId());
        });
    }

    private boolean applyInboundStatus(Listing listing, BitrixDealSnapshot deal) {
        ListingStatus newStatus = statusForStage(deal.getStageId());
        if (newStatus == null || listing.getStatus() == newStatus) {
            return false;
        }
        listing.setStatus(newStatus);
        if (newStatus == ListingStatus.CLOSED && listing.getClosedAt() == null) {
            listing.setClosedAt(Instant.now());
        }
        return true;
    }

    private boolean applyInboundTitle(Listing listing, BitrixDealSnapshot deal) {
        if (deal.getFields() == null || !deal.getFields().containsKey("TITLE")) {
            return false;
        }
        String incoming = normalizeInboundTitle(deal.getField("TITLE"));
        if (Objects.equals(listing.getTitle(), incoming)) {
            return false;
        }
        listing.setTitle(incoming);
        return true;
    }

    private boolean applyInboundPrice(Listing listing, BitrixDealSnapshot deal) {
        if (deal.getFields() == null || !deal.getFields().containsKey("OPPORTUNITY")) {
            return false;
        }
        BigDecimal incoming = toBigDecimal(deal.getField("OPPORTUNITY"));
        if (incoming == null || listing.getPrice().compareTo(incoming) == 0) {
            return false;
        }
        listing.setPrice(incoming);
        return true;
    }

    private boolean applyInboundDescription(Listing listing, BitrixDealSnapshot deal) {
        if (deal.getFields() == null || !deal.getFields().containsKey("COMMENTS")) {
            return false;
        }
        Object raw = deal.getField("COMMENTS");
        String normalized = raw == null || raw.toString().isBlank() ? null : raw.toString();
        if (Objects.equals(listing.getDescription(), normalized)) {
            return false;
        }
        listing.setDescription(normalized);
        return true;
    }

    private boolean applyInboundAddress(Listing listing, BitrixDealSnapshot deal) {
        String field = properties.getDealFieldAddress();
        if (field == null || deal.getFields() == null || !deal.getFields().containsKey(field)) {
            return false;
        }
        Object raw = deal.getField(field);
        String incoming = raw == null || raw.toString().isBlank() ? null : raw.toString();
        if (Objects.equals(listing.getAddress(), incoming)) {
            return false;
        }
        listing.setAddress(incoming);
        return true;
    }

    private boolean applyInboundArea(Listing listing, BitrixDealSnapshot deal) {
        String field = properties.getDealFieldArea();
        if (field == null || deal.getFields() == null || !deal.getFields().containsKey(field)) {
            return false;
        }
        BigDecimal incoming = toBigDecimal(deal.getField(field));
        if (incoming == null) {
            return false;
        }
        if (listing.getAreaSqm() != null && listing.getAreaSqm().compareTo(incoming) == 0) {
            return false;
        }
        listing.setAreaSqm(incoming);
        return true;
    }

    private boolean applyInboundRooms(Listing listing, BitrixDealSnapshot deal) {
        String field = properties.getDealFieldRooms();
        if (field == null || deal.getFields() == null || !deal.getFields().containsKey(field)) {
            return false;
        }
        BigDecimal incoming = toBigDecimal(deal.getField(field));
        if (incoming == null) {
            return false;
        }
        int rooms = incoming.intValue();
        if (listing.getRooms() != null && listing.getRooms() == rooms) {
            return false;
        }
        listing.setRooms(rooms);
        return true;
    }

    private void upsertDealForListing(BitrixConnection conn, Listing managed) throws ResourceException {
        int contactId = ensureContact(conn, managed.getSeller());
        Map<String, Object> fields = dealFields(managed);
        fields.put("STAGE_ID", stageForStatus(managed.getStatus()));
        fields.put("CONTACT_ID", contactId);
        fields.put("CATEGORY_ID", properties.getDealCategoryId());

        Optional<CrmLink> link = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, managed.getId());
        if (link.isPresent()) {
            updateLinkedDeal(conn, link.get(), managed, fields);
        } else {
            int dealId = conn.createDeal(fields);
            saveLink(CrmEntityType.LISTING, managed.getId(), dealId);
        }
    }

    private void updateLinkedDeal(BitrixConnection conn, CrmLink link, Listing listing, Map<String, Object> fields) {
        try {
            conn.updateDeal(link.getBitrixId(), fields);
            touchLink(link);
        } catch (RuntimeException e) {
            if (BitrixDealErrors.isNotFound(e)) {
                deleteListingByBitrixDeletion(listing.getId());
                return;
            }
            throw e;
        }
    }

    private int ensureContact(BitrixConnection conn, User seller) {
        Optional<CrmLink> userLink = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.USER, seller.getId());
        if (userLink.isPresent()) {
            return userLink.get().getBitrixId();
        }
        Optional<Integer> existing = conn.findContactIdByEmail(seller.getEmail());
        int contactId = existing.orElseGet(() -> conn.createContact(seller.getEmail(), seller.getEmail()));
        saveLink(CrmEntityType.USER, seller.getId(), contactId);
        return contactId;
    }

    private Map<String, Object> dealFields(Listing listing) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("TITLE", listing.getTitle());
        fields.put("OPPORTUNITY", listing.getPrice());
        fields.put(properties.getDealFieldListingId(), listing.getId().doubleValue());
        fields.put(properties.getDealFieldPromotion(), promotionLabel(listing.getPromotion()));
        if (listing.getDescription() != null) {
            fields.put("COMMENTS", listing.getDescription());
        }
        putListingCustomFields(fields, listing);
        return fields;
    }

    private void putListingCustomFields(Map<String, Object> fields, Listing listing) {
        if (properties.getDealFieldArea() != null && listing.getAreaSqm() != null) {
            fields.put(properties.getDealFieldArea(), listing.getAreaSqm());
        }
        if (properties.getDealFieldRooms() != null && listing.getRooms() != null) {
            fields.put(properties.getDealFieldRooms(), listing.getRooms().doubleValue());
        }
        if (properties.getDealFieldAddress() != null
                && listing.getAddress() != null
                && !listing.getAddress().isBlank()) {
            fields.put(properties.getDealFieldAddress(), listing.getAddress());
        }
    }

    private String stageForStatus(ListingStatus status) {
        return switch (status) {
            case DRAFT -> properties.getStagePublish();
            case ACTIVE -> properties.getStageActive();
            case CLOSED -> properties.getStageClosed();
            case ARCHIVED -> properties.getStageArchived();
        };
    }

    private ListingStatus statusForStage(String stageId) {
        if (stageId == null) {
            return null;
        }
        if (stageId.equals(properties.getStagePublish())) {
            return ListingStatus.DRAFT;
        }
        if (stageId.equals(properties.getStageActive())) {
            return ListingStatus.ACTIVE;
        }
        if (stageId.equals(properties.getStageClosed())) {
            return ListingStatus.CLOSED;
        }
        if (stageId.equals(properties.getStageArchived())) {
            return ListingStatus.ARCHIVED;
        }
        return null;
    }

    private static String promotionLabel(PromotionType promotion) {
        return promotion != null ? promotion.name() : PromotionType.NONE.name();
    }

    private static String normalizeInboundTitle(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        return raw.toString().trim();
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private void runWithConnection(ConnectionConsumer consumer) {
        try (BitrixConnection conn = connectionFactory.getBitrixConnection()) {
            consumer.accept(conn);
        } catch (ResourceException e) {
            log.error("Bitrix JCA connection failed", e);
        } catch (RuntimeException e) {
            log.warn("Bitrix sync failed: {}", e.getMessage());
        }
    }

    private void saveLink(CrmEntityType type, Long localId, int bitrixId) {
        CrmLink link = new CrmLink();
        link.setEntityType(type);
        link.setLocalId(localId);
        link.setBitrixId(bitrixId);
        link.setLastSyncAt(Instant.now());
        crmLinkRepository.save(link);
    }

    private void touchLink(CrmLink link) {
        link.setLastSyncAt(Instant.now());
        crmLinkRepository.save(link);
    }

    @FunctionalInterface
    private interface ConnectionConsumer {
        void accept(BitrixConnection conn) throws ResourceException;
    }
}
