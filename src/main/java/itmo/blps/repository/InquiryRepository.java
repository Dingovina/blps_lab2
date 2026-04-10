package itmo.blps.repository;

import itmo.blps.entity.Inquiry;
import itmo.blps.entity.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByListingId(Long listingId, Pageable pageable);

    Page<Inquiry> findByBuyerId(Long buyerId, Pageable pageable);

    Page<Inquiry> findByBuyerIdAndStatus(Long buyerId, InquiryStatus status, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.listing.seller.id = :sellerId")
    Page<Inquiry> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.listing.seller.id = :sellerId AND i.status = :status")
    Page<Inquiry> findBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") InquiryStatus status, Pageable pageable);

    boolean existsByListingIdAndBuyerId(Long listingId, Long buyerId);

    Page<Inquiry> findByListingIdAndBuyerId(Long listingId, Long buyerId, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE i.listing.id = :listingId AND i.listing.seller.id = :sellerId")
    Page<Inquiry> findByListingIdAndSellerId(@Param("listingId") Long listingId, @Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT i FROM Inquiry i WHERE (:status IS NULL OR i.status = :status) " +
            "AND (:listingId IS NULL OR i.listing.id = :listingId) AND (:buyerId IS NULL OR i.buyer.id = :buyerId)")
    Page<Inquiry> findAllFiltered(@Param("status") InquiryStatus status,
                                  @Param("listingId") Long listingId,
                                  @Param("buyerId") Long buyerId,
                                  Pageable pageable);
}
