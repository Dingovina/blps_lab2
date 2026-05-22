package itmo.blps.repository;

import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    Page<User> findByRole(UserRole role, Pageable pageable);

    List<User> findByRole(UserRole role);

    @Query(value = "SELECT u.* FROM cian_users u WHERE (:role IS NULL OR u.role = CAST(:role AS TEXT)) AND (:email IS NULL OR LOWER(CAST(u.email AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:email AS TEXT), '%')))",
           countQuery = "SELECT COUNT(*) FROM cian_users u WHERE (:role IS NULL OR u.role = CAST(:role AS TEXT)) AND (:email IS NULL OR LOWER(CAST(u.email AS TEXT)) LIKE LOWER(CONCAT('%', CAST(:email AS TEXT), '%')))",
           nativeQuery = true)
    Page<User> findAllFiltered(@Param("role") String role, @Param("email") String email, Pageable pageable);
}
