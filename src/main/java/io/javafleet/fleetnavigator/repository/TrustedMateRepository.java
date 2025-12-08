package io.javafleet.fleetnavigator.repository;

import io.javafleet.fleetnavigator.model.TrustedMate;
import io.javafleet.fleetnavigator.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TrustedMate entities.
 * Most queries filter by owner (User) for security isolation.
 */
@Repository
public interface TrustedMateRepository extends JpaRepository<TrustedMate, Long> {

    // ===== User-scoped queries (SECURE) =====

    /**
     * Find a trusted mate by mateId for a specific user
     */
    Optional<TrustedMate> findByMateIdAndOwner(String mateId, User owner);

    /**
     * Find a trusted mate by mateId and owner ID
     */
    @Query("SELECT m FROM TrustedMate m WHERE m.mateId = :mateId AND m.owner.id = :ownerId")
    Optional<TrustedMate> findByMateIdAndOwnerId(@Param("mateId") String mateId, @Param("ownerId") Long ownerId);

    /**
     * Find all mates for a specific user
     */
    List<TrustedMate> findByOwner(User owner);

    /**
     * Find all mates for a user by ID
     */
    @Query("SELECT m FROM TrustedMate m WHERE m.owner.id = :ownerId ORDER BY m.lastSeenAt DESC NULLS LAST")
    List<TrustedMate> findByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find enabled mates for a user
     */
    List<TrustedMate> findByOwnerAndEnabledTrue(User owner);

    /**
     * Find mates by type for a user
     */
    List<TrustedMate> findByOwnerAndMateType(User owner, String mateType);

    /**
     * Check if mate exists for user
     */
    boolean existsByMateIdAndOwner(String mateId, User owner);

    /**
     * Delete mate for user
     */
    void deleteByMateIdAndOwner(String mateId, User owner);

    // ===== Global queries (for system operations like WebSocket auth) =====

    /**
     * Find a trusted mate by its unique mateId (global - for auth)
     */
    Optional<TrustedMate> findByMateId(String mateId);

    /**
     * Find a trusted mate by its public key (global - for auth)
     */
    Optional<TrustedMate> findByPublicKey(String publicKey);

    /**
     * Check if a mate with this public key already exists
     */
    boolean existsByPublicKey(String publicKey);

    /**
     * Check if a mate with this mateId already exists (global)
     */
    boolean existsByMateId(String mateId);

    /**
     * Find all enabled mates (global)
     */
    List<TrustedMate> findByEnabledTrue();

    /**
     * Find mates by type (global)
     */
    List<TrustedMate> findByMateType(String mateType);

    /**
     * Delete by mateId (global - careful!)
     */
    void deleteByMateId(String mateId);

    /**
     * Find all mates ordered by last seen (global)
     */
    @Query("SELECT m FROM TrustedMate m ORDER BY m.lastSeenAt DESC NULLS LAST")
    List<TrustedMate> findAllOrderByLastSeen();
}
