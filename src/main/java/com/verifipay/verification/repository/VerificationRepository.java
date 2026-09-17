package com.verifipay.verification.repository;

import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.verification.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VerificationRepository
        extends JpaRepository<Verification, Long>,
        JpaSpecificationExecutor<Verification> {

    List<Verification>
    findAllByCashierIdOrderByCreatedAtDesc(Long cashierId);

    Optional<Verification>
    findByIdAndCashierId(
            Long id,
            Long cashierId
    );

    long countByCompanyIdAndCreatedAtBetween(
            Long companyId,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByCompanyIdAndStatusAndCreatedAtBetween(
            Long companyId,
            VerificationStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Verification>
    findTop10ByCompanyIdOrderByCreatedAtDesc(
            Long companyId
    );

    @Query("""
           SELECT COALESCE(SUM(v.amount), 0)
           FROM Verification v
           WHERE v.company.id = :companyId
             AND v.status = :status
             AND v.createdAt BETWEEN :start AND :end
           """)
    BigDecimal sumAmountByCompanyAndStatusAndCreatedAtBetween(
            @Param("companyId") Long companyId,
            @Param("status") VerificationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}