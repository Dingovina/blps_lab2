package itmo.blps.integration.crm;

import itmo.blps.bitrix.jca.BitrixConnection;
import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.BitrixEventRecord;
import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;
import itmo.blps.config.BitrixProperties;
import itmo.blps.entity.CrmLink;
import itmo.blps.entity.Inquiry;
import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.PromotionType;
import itmo.blps.entity.User;
import itmo.blps.repository.CrmLinkRepository;
import itmo.blps.repository.InquiryRepository;
import itmo.blps.repository.ListingRepository;
import jakarta.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class CrmSyncService {

    private static final Logger log = LoggerFactory.getLogger(CrmSyncService.class);

    private final BitrixConnectionFactoryInterface connectionFactory;
    private final BitrixProperties properties;
    private final CrmLinkRepository crmLinkRepository;
    private final ListingRepository listingRepository;
    private final InquiryRepository inquiryRepository;

    public CrmSyncService(BitrixConnectionFactoryInterface connectionFactory,
                          BitrixProperties properties,
                          CrmLinkRepository crmLinkRepository,
                          ListingRepository listingRepository,
                          InquiryRepository inquiryRepository) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.crmLinkRepository = crmLinkRepository;
        this.listingRepository = listingRepository;
        this.inquiryRepository = inquiryRepository;
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
                conn.updateDeal(existing.get().getBitrixId(), fields);
                touchLink(existing.get());
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
        int inquiries = 0;
        for (Listing listing : listingRepository.findAll()) {
            try {
                syncListingStatus(listing);
                listings++;
            } catch (RuntimeException e) {
                log.warn("Bitrix backfill failed for listing {}: {}", listing.getId(), e.getMessage());
            }
        }
        for (Inquiry inquiry : inquiryRepository.findAll()) {
            if (crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.INQUIRY, inquiry.getId()).isPresent()) {
                continue;
            }
            try {
                syncInquiryCreated(inquiry);
                inquiries++;
            } catch (RuntimeException e) {
                log.warn("Bitrix backfill failed for inquiry {}: {}", inquiry.getId(), e.getMessage());
            }
        }
        log.info("Bitrix backfill finished: {} listings, {} inquiries processed", listings, inquiries);
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
            conn.updateDeal(link.get().getBitrixId(), fields);
            touchLink(link.get());
        }));
    }

    @Transactional
    public void syncInquiryCreated(Inquiry inquiry) {
        if (!properties.isEnabled()) {
            return;
        }
        inquiryRepository.findById(inquiry.getId()).ifPresent(managed -> runWithConnection(conn -> {
            Listing listing = managed.getListing();
            Optional<CrmLink> dealLink = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, listing.getId());
            if (dealLink.isEmpty()) {
                syncListingPublished(listing);
                dealLink = crmLinkRepository.findByEntityTypeAndLocalId(CrmEntityType.LISTING, listing.getId());
            }
            if (dealLink.isEmpty()) {
                return;
            }
            String subject = "Запрос на показ: объявление #" + listing.getId();
            String description = "Покупатель: " + managed.getBuyer().getEmail()
                    + "\nСообщение: " + (managed.getMessage() != null ? managed.getMessage() : "");
            int activityId = conn.addDealActivity(dealLink.get().getBitrixId(), subject, description);
            saveLink(CrmEntityType.INQUIRY, managed.getId(), activityId);
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
            BitrixSyncContext.runInbound(() -> {
                boolean statusChanged = applyInboundStatus(listing, deal);
                boolean priceChanged = applyInboundPrice(listing, deal);
                boolean descriptionChanged = applyInboundDescription(listing, deal);
                boolean addressChanged = applyInboundAddress(listing, deal);
                boolean areaChanged = applyInboundArea(listing, deal);
                boolean roomsChanged = applyInboundRooms(listing, deal);
                if (statusChanged || priceChanged || descriptionChanged
                        || addressChanged || areaChanged || roomsChanged) {
                    listingRepository.save(listing);
                    if (statusChanged) {
                        log.info("Inbound Bitrix: listing {} status -> {}", listingId, listing.getStatus());
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
            conn.updateDeal(link.get().getBitrixId(), fields);
            touchLink(link.get());
        } else {
            int dealId = conn.createDeal(fields);
            saveLink(CrmEntityType.LISTING, managed.getId(), dealId);
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
            log.error("Bitrix sync failed", e);
            throw e;
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
