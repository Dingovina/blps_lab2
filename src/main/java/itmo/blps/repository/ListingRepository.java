package itmo.blps.repository;

import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByStatusAndExpiresAtBefore(ListingStatus status, Instant expiresAt);

    Page<Listing> findBySellerId(Long sellerId, Pageable pageable);

    Page<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status, Pageable pageable);

    boolean existsByIdAndSeller_Id(Long id, Long sellerId);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' " +
            "AND (:region IS NULL OR l.region = :region) " +
            "AND (:minPrice IS NULL OR l.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR l.price <= :maxPrice) " +
            "AND (:rooms IS NULL OR l.rooms = :rooms) " +
            "AND (:minAreaSqm IS NULL OR l.areaSqm >= :minAreaSqm) " +
            "AND (:maxAreaSqm IS NULL OR l.areaSqm <= :maxAreaSqm)")
    Page<Listing> searchActive(@Param("region") String region,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               @Param("rooms") Integer rooms,
                               @Param("minAreaSqm") BigDecimal minAreaSqm,
                               @Param("maxAreaSqm") BigDecimal maxAreaSqm,
                               Pageable pageable);

    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    Page<Listing> findBySeller_Id(Long sellerId, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE (:status IS NULL OR l.status = :status) " +
            "AND (:region IS NULL OR l.region = :region) " +
            "AND (:sellerId IS NULL OR l.seller.id = :sellerId)")
    Page<Listing> findAllFiltered(@Param("status") ListingStatus status,
                                  @Param("region") String region,
                                  @Param("sellerId") Long sellerId,
                                  Pageable pageable);
}
