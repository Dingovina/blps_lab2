package itmo.blps.controller;

import itmo.blps.dto.*;
import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.User;
import itmo.blps.security.AuthUtil;
import itmo.blps.service.ListingService;
import itmo.blps.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ListingController {

    private final ListingService listingService;
    private final PaymentService paymentService;

    public ListingController(ListingService listingService, PaymentService paymentService) {
        this.listingService = listingService;
        this.paymentService = paymentService;
    }

    @PreAuthorize("hasAuthority('PRIV_CREATE_LISTING') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/listings")
    public ResponseEntity<ListingResponse> create(@Valid @RequestBody ListingCreateRequest request) {
        User user = AuthUtil.currentUser();
        Listing l = listingService.create(user, request);
        return ResponseEntity.status(201).body(ListingResponse.from(l));
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PutMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ListingCreateRequest request) {
        User user = AuthUtil.currentUser();
        Listing l = listingService.update(id, user, request);
        return ResponseEntity.ok(ListingResponse.from(l));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingResponse> get(@PathVariable Long id) {
        Listing l = listingService.getById(id);
        return ResponseEntity.ok(ListingResponse.from(l));
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @GetMapping("/seller/listings")
    public ResponseEntity<PageResponse<ListingResponse>> myListings(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = AuthUtil.currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = listingService.findBySeller(user.getId(), status, pageable);
        List<ListingResponse> content = p.getContent().stream().map(ListingResponse::from).collect(Collectors.toList());
        PageResponse<ListingResponse> resp = new PageResponse<>(
                content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/listings/{id}/promotion/choice")
    public ResponseEntity<?> promotionChoice(@PathVariable Long id,
                                             @RequestBody java.util.Map<String, Boolean> body) {
        User user = AuthUtil.currentUser();
        listingService.getListingOwnedBy(id, user);
        boolean withPromotion = body != null && Boolean.TRUE.equals(body.get("withPromotion"));
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("listingId", id);
        resp.put("withPromotion", withPromotion);
        resp.put("nextStep", withPromotion ? "pay" : "none");
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/listings/{id}/confirm-relevance")
    public ResponseEntity<?> confirmRelevance(@PathVariable Long id,
                                             @RequestBody(required = false) ConfirmRelevanceRequest request) {
        User user = AuthUtil.currentUser();
        boolean relevant = request == null || request.isRelevant();
        Listing l = listingService.confirmRelevance(id, user, relevant);
        var respBody = new java.util.LinkedHashMap<String, Object>();
        respBody.put("listingId", l.getId());
        respBody.put("action", relevant ? "EXTENDED" : "ARCHIVED");
        respBody.put("expiresAt", l.getExpiresAt());
        return ResponseEntity.ok(respBody);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/listings/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id) {
        User user = AuthUtil.currentUser();
        Listing l = listingService.close(id, user);
        var respBody = new java.util.LinkedHashMap<String, Object>();
        respBody.put("listingId", l.getId());
        respBody.put("status", l.getStatus().name());
        respBody.put("closedAt", l.getClosedAt());
        return ResponseEntity.ok(respBody);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_OWN_LISTINGS') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/listings/{id}/promotion/pay")
    public ResponseEntity<?> promotionPay(@PathVariable Long id,
                                         @Valid @RequestBody PromotionPayRequest request) {
        User user = AuthUtil.currentUser();
        var result = paymentService.pay(id, user, request.getPromotionType());
        var respBody = new java.util.LinkedHashMap<String, Object>();
        respBody.put("paymentId", result.paymentId());
        respBody.put("status", result.status().name());
        respBody.put("message", result.message());
        return ResponseEntity.ok(respBody);
    }

    @GetMapping("/listings/search")
    public ResponseEntity<PageResponse<ListingResponse>> search(
            @RequestParam(required = false) List<String> filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("promotion"), Sort.Order.desc("publishedAt")));
        var p = listingService.search(filter, pageable);
        List<ListingResponse> content = p.getContent().stream().map(ListingResponse::from).collect(Collectors.toList());
        PageResponse<ListingResponse> resp = new PageResponse<>(
                content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }
}
