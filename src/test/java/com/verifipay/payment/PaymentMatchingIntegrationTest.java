package com.verifipay.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMatchingIntegrationTest extends PaymentMatchingTestSupport {
    @Test
    void delayedPaymentMatchesAtEventTimeAndPaidAtUsesBackendTime() {
        var id = request("WAITING", null);
        var before = Instant.now().minusMillis(1);
        var event = receive(start.plusSeconds(240));
        assertEquals("PAID", requestStatus(id));
        assertEquals("MATCHED", eventStatus(event));
        assertEquals(id, jdbc.queryForObject("SELECT payment_request_id FROM payment_events WHERE id = ?", Long.class, event));
        assertFalse(requestTime(id, "paid_at").isBefore(before));
        assertFalse(requestTime(id, "paid_at").isAfter(Instant.now()));
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "299999,true", "300000,false", "300001,false"})
    void temporalBoundaries(long milliseconds, boolean matches) {
        var id = request("WAITING", null);
        var event = receive(start.plusMillis(milliseconds));
        assertEquals(matches ? "MATCHED" : "UNMATCHED", eventStatus(event));
        assertEquals(matches ? "PAID" : "WAITING", requestStatus(id));
    }

    @ParameterizedTest
    @CsvSource({"WAITING,119999,true", "WAITING,120000,true", "WAITING,120001,true",
            "CANCELLED,119999,true", "CANCELLED,120000,false", "CANCELLED,120001,false"})
    void cancellationIsEvaluatedAtEventTime(String status, long milliseconds, boolean matches) {
        var cancelled = start.plusSeconds(120);
        var id = request(status, cancelled);
        var event = receive(start.plusMillis(milliseconds));
        assertEquals(matches ? "MATCHED" : "UNMATCHED", eventStatus(event));
        assertEquals(matches ? "PAID" : status, requestStatus(id));
        assertEquals(cancelled, requestTime(id, "cancelled_at"));
        assertEquals(matches, requestTime(id, "paid_at") != null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PAID", "AMBIGUOUS", "EXPIRED", "CANCELLED"})
    void ineligibleStatesAndHistoricalCancellationAreExcluded(String state) {
        var id = request(state, null);
        assertEquals("UNMATCHED", eventStatus(receive(start.plusSeconds(60))));
        assertEquals(state, requestStatus(id));
    }

    @Test
    void paidAmbiguousAndExpiredRemainExcludedWithKnownCancellation() {
        for (var state : List.of("PAID", "AMBIGUOUS", "EXPIRED")) {
            request(state, start.plusSeconds(120));
        }
        assertEquals("UNMATCHED", eventStatus(receive(start.plusSeconds(60))));
    }

    @Test
    void cancellationDoesNotOverrideCreationAndExpirationWindow() {
        var id = request("CANCELLED", start.plusSeconds(600));
        assertEquals("UNMATCHED", eventStatus(receive(start.minusMillis(1))));
        assertEquals("UNMATCHED", eventStatus(receive(start.plusSeconds(300))));
        assertEquals("CANCELLED", requestStatus(id));
    }

    @Test
    void ambiguityKeepsAllCandidatesAndCancellationEvidenceAcrossBranchesAndCashiers() throws Exception {
        var first = request("WAITING", null);
        var second = request("CANCELLED", start.plusSeconds(180));
        var otherBranch = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Other') RETURNING id",
                Long.class, companyId);
        jdbc.update("UPDATE payment_requests SET branch_id = ?, cashier_id = ? WHERE id = ?",
                otherBranch, newCashier(otherBranch), second);
        var event = receive(start.plusSeconds(60));
        assertEquals("AMBIGUOUS", eventStatus(event));
        assertEquals(List.of(first, second), jdbc.queryForList(
                "SELECT payment_request_id FROM payment_match_candidates WHERE payment_event_id = ? ORDER BY payment_request_id",
                Long.class, event));
        for (var id : List.of(first, second)) {
            assertEquals("AMBIGUOUS", requestStatus(id));
            assertNull(requestTime(id, "paid_at"));
        }
        assertEquals(start.plusSeconds(180), requestTime(second, "cancelled_at"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> asCashier(() -> requests.cancel(first))).getStatusCode().value());
        assertEquals("UNMATCHED", eventStatus(receive(start.plusSeconds(61))));
    }

    @Test
    void ineligibleCancelledRequestDoesNotCreateFalseAmbiguity() {
        var waiting = request("WAITING", null);
        request("CANCELLED", start.plusSeconds(30));
        receive(start.plusSeconds(60));
        assertEquals("PAID", requestStatus(waiting));
    }

    @Test
    void ignoresDifferentCompanyAndAmountButNotSenderOrSecurityCode() {
        var otherCompany = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Other') RETURNING id", Long.class);
        var foreign = request("WAITING", null);
        jdbc.update("UPDATE payment_requests SET company_id = ? WHERE id = ?", otherCompany, foreign);
        var differentAmount = request("WAITING", null);
        jdbc.update("UPDATE payment_requests SET amount = 81 WHERE id = ?", differentAmount);
        var own = request("WAITING", null);
        var p = payload(UUID.randomUUID().toString(), start.plusSeconds(60));
        var event = events.receiveYapeEvent(token, new com.verifipay.device.dto.YapeDeviceEventRequest(
                p.amount(), "different", "other sender", p.occurredAtEpochMillis(), p.sourceEventId())).paymentEventId();
        assertEquals("MATCHED", eventStatus(event));
        assertEquals("PAID", requestStatus(own));
        assertEquals("WAITING", requestStatus(foreign));
        assertEquals("WAITING", requestStatus(differentAmount));
    }

    @Test
    void cancellationTimestampIsStableAndHistoricalPaymentCanPayIt() throws Exception {
        var id = request("WAITING", null);
        asCashier(() -> requests.cancel(id));
        var cancelled = requestTime(id, "cancelled_at");
        assertNotNull(cancelled);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> asCashier(() -> requests.cancel(id))).getStatusCode().value());
        assertEquals(cancelled, requestTime(id, "cancelled_at"));
        var key = UUID.randomUUID().toString();
        var payload = payload(key, start.plusSeconds(60));
        var event = events.receiveYapeEvent(token, payload);
        assertEquals("PAID", requestStatus(id));
        var paid = requestTime(id, "paid_at");
        assertEquals(event.paymentEventId(), events.receiveYapeEvent(token, payload).paymentEventId());
        assertEquals(paid, requestTime(id, "paid_at"));
        assertEquals(cancelled, requestTime(id, "cancelled_at"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> asCashier(() -> requests.cancel(id))).getStatusCode().value());
    }

    @Test
    void twoConcurrentEventsCanOnlyPayOnce() throws Exception {
        var id = request("WAITING", null);
        var barrier = new CyclicBarrier(2);
        Callable<Long> send = () -> { barrier.await(5, TimeUnit.SECONDS); return receive(start.plusSeconds(60)); };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.of(send, send), 15, TimeUnit.SECONDS);
            var states = List.of(eventStatus(results.get(0).get()), eventStatus(results.get(1).get())).stream().sorted().toList();
            assertEquals(List.of("MATCHED", "UNMATCHED"), states);
        }
        assertEquals("PAID", requestStatus(id));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE payment_request_id = ?", Long.class, id));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cancellationAndMatchingRespectCompanyLockInBothOrders(boolean cancelFirst) throws Exception {
        var id = request("WAITING", null);
        var entered = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<?>[] pending = new Future<?>[1];
            transaction().executeWithoutResult(tx -> {
                jdbc.queryForObject("SELECT id FROM companies WHERE id = ? FOR UPDATE", Long.class, companyId);
                try {
                    if (cancelFirst) asCashier(() -> requests.cancel(id));
                    else receive(start.plusSeconds(60));
                } catch (Exception ex) { throw new RuntimeException(ex); }
                pending[0] = pool.submit(() -> {
                    entered.countDown();
                    if (cancelFirst) return receive(start.plusSeconds(60));
                    return assertThrows(ResponseStatusException.class, () -> asCashier(() -> requests.cancel(id))).getStatusCode().value();
                });
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> pending[0].get(200, TimeUnit.MILLISECONDS));
                } catch (InterruptedException ex) { throw new RuntimeException(ex); }
            });
            pending[0].get(10, TimeUnit.SECONDS);
        }
        assertEquals("PAID", requestStatus(id));
        assertEquals(cancelFirst, requestTime(id, "cancelled_at") != null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creationAndMatchingRespectCompanyLockInBothOrders(boolean createFirst) throws Exception {
        // A future timestamp makes the new request temporally compatible in either ordering.
        var at = Instant.now().plusSeconds(30);
        var entered = new CountDownLatch(1);
        Long[] id = new Long[1];
        Long[] event = new Long[1];
        try (var pool = Executors.newSingleThreadExecutor()) {
            Future<Long>[] pending = new Future[1];
            transaction().executeWithoutResult(tx -> {
                jdbc.queryForObject("SELECT id FROM companies WHERE id = ? FOR UPDATE", Long.class, companyId);
                try {
                    if (createFirst) id[0] = createRequest();
                    else event[0] = receive(at);
                } catch (Exception ex) { throw new RuntimeException(ex); }
                pending[0] = pool.submit(() -> { entered.countDown(); return createFirst ? receive(at) : createRequest(); });
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> pending[0].get(200, TimeUnit.MILLISECONDS));
                } catch (InterruptedException ex) { throw new RuntimeException(ex); }
            });
            if (createFirst) event[0] = pending[0].get(10, TimeUnit.SECONDS);
            else id[0] = pending[0].get(10, TimeUnit.SECONDS);
        }
        assertEquals(createFirst ? "MATCHED" : "UNMATCHED", eventStatus(event[0]));
        assertEquals(createFirst ? "PAID" : "WAITING", requestStatus(id[0]));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void rollbackLeavesNoEventAssignmentOrCandidates(int count) {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < count; i++) ids.add(request("WAITING", null));
        assertThrows(IllegalStateException.class, () -> transaction().executeWithoutResult(tx -> {
            receive(start.plusSeconds(60));
            throw new IllegalStateException("Force rollback after matching");
        }));
        for (var id : ids) assertEquals("WAITING", requestStatus(id));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ?", Long.class, companyId));
        assertEquals(0L, jdbc.queryForObject("""
                SELECT count(*) FROM payment_match_candidates c JOIN payment_requests p ON p.id = c.payment_request_id
                WHERE p.company_id = ?
                """, Long.class, companyId));
    }

    @Test
    void databaseRejectsDoubleAssignmentAndInvalidEventStates() {
        var request = request("WAITING", null);
        var matched = receive(start.plusSeconds(60));
        var unmatched = receive(start.plusSeconds(61));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE payment_events SET matching_status = 'MATCHED', payment_request_id = ? WHERE id = ?", request, unmatched));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE payment_events SET matching_status = 'UNMATCHED' WHERE id = ?", matched));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE payment_events SET matching_status = null WHERE id = ?", matched));
        for (String assignment : List.of("matching_status = 'INVALID'", "matching_status = 'MATCHED'",
                "occurred_at_instant = null", "source_event_id = null")) {
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                    "UPDATE payment_events SET " + assignment + " WHERE id = ?", unmatched));
        }
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE payment_events SET matching_status = 'MATCHED', payment_request_id = -1 WHERE id = ?", unmatched));
        jdbc.update("INSERT INTO payment_match_candidates(payment_event_id, payment_request_id) VALUES (?, ?)", unmatched, request);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO payment_match_candidates(payment_event_id, payment_request_id) VALUES (?, ?)", unmatched, request));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO payment_match_candidates(payment_event_id, payment_request_id) VALUES (-1, ?)", request));
    }
}
