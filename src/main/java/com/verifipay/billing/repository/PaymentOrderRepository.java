package com.verifipay.billing.repository;
import com.verifipay.billing.entity.PaymentOrder;
import com.verifipay.common.enums.PaymentOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
    Optional<PaymentOrder> findBySubscriptionIdAndStatus(Long subscriptionId, PaymentOrderStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PaymentOrder p where p.id = :id")
    Optional<PaymentOrder> findByIdForUpdate(@Param("id") Long id);
}
