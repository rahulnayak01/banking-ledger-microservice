package com.example.ledger.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository
        extends JpaRepository<AccountBalance, UUID> {

    /**
     * Acquires an exclusive row-level lock (PostgreSQL SELECT ... FOR UPDATE).
     *
     * <p>Prevents concurrent reads/writes on this balance row until the current
     * database transaction completes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM AccountBalance b WHERE b.accountId = :accountId")
    Optional<AccountBalance> findByIdForUpdate(@Param("accountId") UUID accountId);
}