package itmo.blps.controller;

import itmo.blps.dto.*;
import itmo.blps.entity.*;
import itmo.blps.repository.InquiryRepository;
import itmo.blps.repository.ListingRepository;
import itmo.blps.repository.PaymentRepository;
import itmo.blps.repository.UserRepository;
import itmo.blps.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('PRIV_ADMIN_ACCESS')")
public class AdminController {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final InquiryRepository inquiryRepository;
    private final PaymentRepository paymentRepository;
    private final ListingService listingService;

    public AdminController(UserRepository userRepository,
                           ListingRepository listingRepository,
                           InquiryRepository inquiryRepository,
                           PaymentRepository paymentRepository,
                           ListingService listingService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.inquiryRepository = inquiryRepository;
        this.paymentRepository = paymentRepository;
        this.listingService = listingService;
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> users(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var p = userRepository.findAllFiltered(role, email, pageable);
        List<UserResponse> content = p.getContent().stream().map(UserResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize()));
    }

    @GetMapping("/listings")
    public ResponseEntity<PageResponse<ListingResponse>> listings(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = listingRepository.findAllFiltered(status, region, sellerId, pageable);
        List<ListingResponse> content = p.getContent().stream().map(ListingResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize()));
    }

    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<InquiryResponse>> inquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Long listingId,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = inquiryRepository.findAllFiltered(status, listingId, buyerId, pageable);
        List<InquiryResponse> content = p.getContent().stream().map(InquiryResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize()));
    }

    @GetMapping("/payments")
    public ResponseEntity<PageResponse<PaymentResponse>> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var p = paymentRepository.findAllFiltered(status, listingId, pageable);
        List<PaymentResponse> content = p.getContent().stream().map(PaymentResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize()));
    }

    @PostMapping("/listings/{id}/publish")
    public ResponseEntity<?> publishListing(@PathVariable Long id,
                                            @RequestBody(required = false) itmo.blps.dto.PublishRequest request) {
        boolean forceReject = request != null && request.isForceReject();
        Listing l = listingService.publish(id, forceReject);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("id", l.getId());
        body.put("status", l.getStatus().name());
        body.put("publishedAt", l.getPublishedAt());
        body.put("message", "Объявление размещено. Доступно платное продвижение (Топ/Премиум).");
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/listings/{id}/status")
    public ResponseEntity<ListingResponse> setListingStatus(@PathVariable Long id,
                                                             @Valid @RequestBody AdminListingStatusRequest request) {
        Listing l = listingService.adminSetStatus(id, request.getStatus());
        return ResponseEntity.ok(ListingResponse.from(l));
    }
}
