package itmo.blps.repository;

import itmo.blps.entity.Payment;
import itmo.blps.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByListingId(Long listingId);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByListing_Id(Long listingId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE (:status IS NULL OR p.status = :status) AND (:listingId IS NULL OR p.listing.id = :listingId)")
    Page<Payment> findAllFiltered(@Param("status") PaymentStatus status, @Param("listingId") Long listingId, Pageable pageable);
}
