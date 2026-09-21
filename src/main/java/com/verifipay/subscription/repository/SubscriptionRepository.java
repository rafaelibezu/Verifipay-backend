package com.verifipay.subscription.repository;
import com.verifipay.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByCompanyIdAndEndedAtIsNull(Long companyId);
    boolean existsByCompanyIdAndEndedAtIsNull(Long companyId);
    List<Subscription> findByCompanyIdOrderByCreatedAtDescIdDesc(Long companyId);
}
