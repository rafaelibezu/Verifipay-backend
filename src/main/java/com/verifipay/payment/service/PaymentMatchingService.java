package com.verifipay.payment.service;

import com.verifipay.common.enums.PaymentEventStatus;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.payment.entity.PaymentMatchCandidate;
import com.verifipay.payment.repository.PaymentMatchCandidateRepository;
import com.verifipay.paymentrequest.repository.PaymentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PaymentMatchingService {
    private final PaymentRequestRepository requests;
    private final PaymentMatchCandidateRepository candidates;
    private final Clock clock;

    @Autowired
    public PaymentMatchingService(PaymentRequestRepository requests, PaymentMatchCandidateRepository candidates) {
        this(requests, candidates, Clock.systemUTC());
    }

    PaymentMatchingService(PaymentRequestRepository requests, PaymentMatchCandidateRepository candidates, Clock clock) {
        this.requests = requests;
        this.candidates = candidates;
        this.clock = clock;
    }

    // Called only after a successful new insert, with Company and event already locked.
    // Duplicate deliveries must never call this method, including UNMATCHED events.
    @Transactional(propagation = Propagation.MANDATORY)
    public void matchNewEvent(PaymentEvent event) {
        if (event.getMatchingStatus() != PaymentEventStatus.UNMATCHED || event.getPaymentRequest() != null) {
            throw new IllegalStateException("Solo un evento nuevo admite matching");
        }
        var matches = requests.findCandidatesForUpdate(
                event.getCompany().getId(), event.getAmount(), event.getOccurredAtInstant());
        if (matches.size() == 1) {
            var request = matches.getFirst();
            request.markPaid(event.getOccurredAtInstant(), clock.instant());
            event.match(request);
        } else if (matches.size() > 1) {
            event.markAmbiguous();
            for (var request : matches) {
                request.markAmbiguous(event.getOccurredAtInstant());
                candidates.save(new PaymentMatchCandidate(event, request));
            }
        }
    }
}
