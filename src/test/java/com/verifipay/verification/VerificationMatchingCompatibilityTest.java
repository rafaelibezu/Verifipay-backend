package com.verifipay.verification;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.payment.PaymentMatchingTestSupport;
import com.verifipay.verification.dto.CreateVerificationRequest;
import com.verifipay.verification.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class VerificationMatchingCompatibilityTest extends PaymentMatchingTestSupport {
    @Autowired VerificationService verification;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothLegacyQueriesExcludeAllNewStates(boolean withCode) throws Exception {
        var unmatched = receive(start);
        var request = request("WAITING", null);
        var matched = receive(start);
        request("WAITING", null);
        request("WAITING", null);
        var ambiguous = receive(start);
        // Put all events inside Verification's legacy search window, independent of host timezone.
        for (var id : List.of(unmatched, matched, ambiguous)) {
            jdbc.update("UPDATE payment_events SET occurred_at = ? WHERE id = ?", LocalDateTime.now(), id);
        }
        var input = new CreateVerificationRequest(PaymentMethod.YAPE, new BigDecimal("80"), null, withCode ? "123" : null);
        assertEquals(VerificationStatus.NOT_FOUND, asCashier(() -> verification.verify(input)).status());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ? AND consumed", Long.class, companyId));
        assertEquals("PAID", requestStatus(request));
        var legacy = jdbc.queryForObject("""
                INSERT INTO payment_events(company_id, branch_id, method, amount, security_code, occurred_at)
                VALUES (?, ?, 'YAPE', 80, '123', ?) RETURNING id
                """, Long.class, companyId, branchId, LocalDateTime.now());
        assertEquals(VerificationStatus.DETECTED, asCashier(() -> verification.verify(input)).status());
        assertTrue(jdbc.queryForObject("SELECT consumed FROM payment_events WHERE id = ?", Boolean.class, legacy));
        assertNull(eventStatus(legacy));
    }

    @Test
    void verificationAndMatchingConsumeSeparateEventsConcurrently() throws Exception {
        var id = request("WAITING", null);
        var legacy = jdbc.queryForObject("""
                INSERT INTO payment_events(company_id, branch_id, method, amount, occurred_at)
                VALUES (?, ?, 'YAPE', 80, ?) RETURNING id
                """, Long.class, companyId, branchId, LocalDateTime.now());
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.<Callable<Object>>of(
                    () -> { barrier.await(5, TimeUnit.SECONDS); return receive(start.plusSeconds(60)); },
                    () -> { barrier.await(5, TimeUnit.SECONDS); return asCashier(() -> verification.verify(
                            new CreateVerificationRequest(PaymentMethod.YAPE, new BigDecimal("80"), null, null))); }), 15, TimeUnit.SECONDS);
            for (var result : results) assertNotNull(result.get());
        }
        assertEquals("PAID", requestStatus(id));
        assertTrue(jdbc.queryForObject("SELECT consumed FROM payment_events WHERE id = ?", Boolean.class, legacy));
        assertNull(jdbc.queryForObject("SELECT payment_request_id FROM payment_events WHERE id = ?", Long.class, legacy));
        assertEquals(legacy, jdbc.queryForObject("SELECT payment_event_id FROM verifications WHERE company_id = ?", Long.class, companyId));
    }

    @Test
    void legacyDuplicateAndVerificationUseSameLockOrder() throws Exception {
        var at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var key = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO payment_events(company_id, branch_id, method, amount, reference, security_code, occurred_at, source_event_id)
                VALUES (?, ?, 'YAPE', 80, 'Sender', '123', ?, ?)
                """, companyId, branchId, LocalDateTime.ofInstant(at, ZoneId.of("America/Lima")), key);
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.<Callable<Object>>of(
                    () -> { barrier.await(5, TimeUnit.SECONDS); return events.receiveYapeEvent(token, payload(key, at)); },
                    () -> { barrier.await(5, TimeUnit.SECONDS); return asCashier(() -> verification.verify(
                            new CreateVerificationRequest(PaymentMethod.YAPE, new BigDecimal("80"), null, "123"))); }), 15, TimeUnit.SECONDS);
            for (var result : results) assertNotNull(result.get());
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE source_event_id = ? AND matching_status IS NULL", Long.class, key));
    }
}
