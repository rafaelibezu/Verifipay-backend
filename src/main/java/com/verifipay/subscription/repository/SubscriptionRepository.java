package com.verifipay.subscription.repository;
import com.verifipay.subscription.entity.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByCompanyIdAndEndedAtIsNull(Long companyId);
    boolean existsByCompanyIdAndEndedAtIsNull(Long companyId);
    List<Subscription> findByCompanyIdOrderByCreatedAtDescIdDesc(Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);
}
