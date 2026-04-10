package itmo.blps.controller;

import itmo.blps.dto.*;
import itmo.blps.entity.Inquiry;
import itmo.blps.entity.InquiryStatus;
import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import itmo.blps.security.AuthUtil;
import itmo.blps.service.InquiryService;
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
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PreAuthorize("hasAuthority('PRIV_CREATE_INQUIRY') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/inquiries")
    public ResponseEntity<InquiryResponse> create(@Valid @RequestBody InquiryCreateRequest request) {
        User user = AuthUtil.currentUser();
        Inquiry i = inquiryService.create(user, request.getListingId(), request.getMessage());
        return ResponseEntity.status(201).body(InquiryResponse.from(i));
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_INQUIRIES') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @GetMapping("/inquiries/{id}")
    public ResponseEntity<InquiryResponse> get(@PathVariable Long id) {
        Inquiry i = inquiryService.getById(id);
        return ResponseEntity.ok(InquiryResponse.from(i));
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_INQUIRIES') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @GetMapping("/inquiries")
    public ResponseEntity<PageResponse<InquiryResponse>> list(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = AuthUtil.currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Inquiry> p;
        if (user.getRole() == UserRole.SELLER || user.getRole() == UserRole.ADMIN) {
            p = inquiryService.findBySeller(user.getId(), status, pageable);
        } else {
            p = inquiryService.findByBuyer(user.getId(), status, pageable);
        }
        List<InquiryResponse> content = p.getContent().stream().map(InquiryResponse::from).collect(Collectors.toList());
        PageResponse<InquiryResponse> resp = new PageResponse<>(content, p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_INQUIRIES') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/inquiries/{id}/showing-decision")
    public ResponseEntity<?> showingDecision(@PathVariable Long id,
                                             @Valid @RequestBody ShowingDecisionRequest request) {
        User user = AuthUtil.currentUser();
        Inquiry i = inquiryService.resolveShowing(id, user,
                request.getDecision(),
                request.getScheduledAt(),
                request.getContactInfo(),
                request.getReason());
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("inquiryId", i.getId());
        body.put("status", i.getStatus().name());
        body.put("scheduledAt", i.getScheduledAt());
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasAuthority('PRIV_MANAGE_INQUIRIES') or hasAuthority('PRIV_ADMIN_ACCESS')")
    @PostMapping("/inquiries/{id}/visit-result")
    public ResponseEntity<?> visitResult(@PathVariable Long id,
                                         @RequestBody(required = false) VisitResultRequest request) {
        User user = AuthUtil.currentUser();
        Boolean willBuy = request != null ? request.getWillBuy() : null;
        Inquiry i = inquiryService.visitResult(id, user, willBuy);
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("inquiryId", i.getId());
        body.put("willBuy", Boolean.TRUE.equals(i.getWillBuy()));
        body.put("status", i.getStatus().name());
        return ResponseEntity.ok(body);
    }
}
