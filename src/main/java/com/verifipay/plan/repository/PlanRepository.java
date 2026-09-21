package com.verifipay.plan.repository;
import com.verifipay.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PlanRepository extends JpaRepository<Plan, Long> {
    boolean existsByCode(String code);
}
