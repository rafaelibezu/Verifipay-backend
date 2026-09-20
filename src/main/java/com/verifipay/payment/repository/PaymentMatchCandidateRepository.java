package com.verifipay.payment.repository;

import com.verifipay.payment.entity.PaymentMatchCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMatchCandidateRepository extends JpaRepository<PaymentMatchCandidate, Long> {
}
