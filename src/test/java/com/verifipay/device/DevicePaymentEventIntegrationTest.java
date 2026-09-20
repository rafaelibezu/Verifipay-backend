package com.verifipay.device;

import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.dto.YapeDeviceEventResponse;
import com.verifipay.payment.PaymentMatchingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DevicePaymentEventIntegrationTest extends PaymentMatchingTestSupport {
    private static final String URL = "/api/v1/device/payment-events/yape";
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void web() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void epochIsStoredWithoutTimezoneShiftAndLegacyUsesLima() {
        var at = start.plusMillis(123);
        var event = receive(at);
        assertEquals(at, jdbc.queryForObject("SELECT occurred_at_instant FROM payment_events WHERE id = ?",
                (rs, row) -> rs.getTimestamp(1).toInstant(), event));
        assertEquals(LocalDateTime.ofInstant(at, ZoneId.of("America/Lima")), jdbc.queryForObject(
                "SELECT occurred_at FROM payment_events WHERE id = ?", LocalDateTime.class, event));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void duplicateKeepsPersistedResultWithoutRematching(int candidates) {
        for (int i = 0; i < candidates; i++) request("WAITING", null);
        var p = payload(UUID.randomUUID().toString(), start.plusSeconds(60));
        var first = events.receiveYapeEvent(token, p);
        var state = eventStatus(first.paymentEventId());
        // A newly compatible request must not be consumed by a retry of any previous result.
        var later = request("WAITING", null);
        var retry = events.receiveYapeEvent(token, new YapeDeviceEventRequest(
                new BigDecimal("80"), p.securityCode(), p.sender(), p.occurredAtEpochMillis(), p.sourceEventId()));
        assertFalse(first.duplicate());
        assertTrue(retry.duplicate());
        assertEquals(first.paymentEventId(), retry.paymentEventId());
        assertEquals(state, eventStatus(retry.paymentEventId()));
        assertEquals("WAITING", requestStatus(later));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE source_event_id = ?", Long.class, p.sourceEventId()));
    }

    @Test
    void creatingRequestDoesNotReprocessUnmatchedEvent() throws Exception {
        var at = Instant.now().plusSeconds(30);
        var p = payload(UUID.randomUUID().toString(), at);
        var event = events.receiveYapeEvent(token, p);
        var id = createRequest();
        events.receiveYapeEvent(token, p);
        assertEquals("UNMATCHED", eventStatus(event.paymentEventId()));
        assertEquals("WAITING", requestStatus(id));
    }

    @Test
    void concurrentIdenticalDeliveriesCreateExactlyOneEventAndAssignment() throws Exception {
        var id = request("WAITING", null);
        var p = payload(UUID.randomUUID().toString(), start.plusSeconds(60));
        var barrier = new CyclicBarrier(6);
        try (var pool = Executors.newFixedThreadPool(6)) {
            var tasks = java.util.stream.IntStream.range(0, 6).<Callable<YapeDeviceEventResponse>>mapToObj(i -> () -> {
                barrier.await(5, TimeUnit.SECONDS);
                return events.receiveYapeEvent(token, p);
            }).toList();
            var values = new java.util.ArrayList<YapeDeviceEventResponse>();
            for (var result : pool.invokeAll(tasks, 20, TimeUnit.SECONDS)) values.add(result.get());
            assertEquals(1, values.stream().filter(v -> !v.duplicate()).count());
            assertEquals(1, values.stream().map(YapeDeviceEventResponse::paymentEventId).distinct().count());
        }
        assertEquals("PAID", requestStatus(id));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE payment_request_id = ?", Long.class, id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "time", "sender", "securityCode"})
    void incompatibleDuplicateReturns409(String field) throws Exception {
        var key = UUID.randomUUID().toString();
        events.receiveYapeEvent(token, payload(key, start));
        String json = "{\"amount\":" + (field.equals("amount") ? "81" : "80")
                + ",\"occurredAtEpochMillis\":" + (start.toEpochMilli() + (field.equals("time") ? 1 : 0))
                + ",\"sender\":\"" + (field.equals("sender") ? "Changed" : "Sender")
                + "\",\"securityCode\":\"" + (field.equals("securityCode") ? "999" : "123")
                + "\",\"sourceEventId\":\"" + key + "\"}";
        mvc.perform(post(URL).header("X-Device-Token", token).contentType("application/json").content(json))
                .andExpect(status().isConflict());
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ?", Long.class, companyId));
    }

    @Test
    void concurrentDifferentPayloadsHaveOneWinnerAndOneConflict() throws Exception {
        var key = UUID.randomUUID().toString();
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = List.of("80", "81").stream().<Callable<Integer>>map(amount -> () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    events.receiveYapeEvent(token, new YapeDeviceEventRequest(new BigDecimal(amount), "123", "Sender", start.toEpochMilli(), key));
                    return 200;
                } catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            }).toList();
            var results = pool.invokeAll(tasks, 15, TimeUnit.SECONDS);
            assertEquals(List.of(200, 409), List.of(results.get(0).get(), results.get(1).get()).stream().sorted().toList());
        }
    }

    @Test
    void duplicateFromAnotherCompanyIsRejected() {
        var p = payload(UUID.randomUUID().toString(), start);
        events.receiveYapeEvent(token, p);
        var other = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Other') RETURNING id", Long.class);
        var branch = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Other') RETURNING id", Long.class, other);
        var otherToken = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO receiver_devices(company_id, branch_id, name, token_hash) VALUES (?, ?, 'Other', ?)",
                other, branch, tokens.hash(otherToken));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> events.receiveYapeEvent(otherToken, p)).getStatusCode().value());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ?", Long.class, other));
    }

    @Test
    void concurrentSourceIdCollisionAcrossCompaniesHasOneWinner() throws Exception {
        var other = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Other') RETURNING id", Long.class);
        var branch = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Other') RETURNING id", Long.class, other);
        var otherToken = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO receiver_devices(company_id, branch_id, name, token_hash) VALUES (?, ?, 'Other', ?)",
                other, branch, tokens.hash(otherToken));
        var p = payload(UUID.randomUUID().toString(), start);
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(token, otherToken).stream().<Callable<Integer>>map(deviceToken -> () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    assertFalse(events.receiveYapeEvent(deviceToken, p).duplicate());
                    return 200;
                } catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            }).toList();
            var results = pool.invokeAll(tasks, 15, TimeUnit.SECONDS);
            assertEquals(List.of(200, 409), List.of(results.get(0).get(), results.get(1).get()).stream().sorted().toList());
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE source_event_id = ?",
                Long.class, p.sourceEventId()));
    }

    @Test
    void legacyRetryReturnsExistingIdWithoutBackfillOrMatching() {
        var key = UUID.randomUUID().toString();
        var local = LocalDateTime.ofInstant(start, ZoneId.of("America/Lima"));
        var event = jdbc.queryForObject("""
                INSERT INTO payment_events(company_id, branch_id, method, amount, reference, security_code,
                    occurred_at, source_event_id, consumed) VALUES (?, ?, 'YAPE', 80, 'Sender', '123', ?, ?, true) RETURNING id
                """, Long.class, companyId, branchId, Timestamp.valueOf(local), key);
        var id = request("WAITING", null);
        var retry = events.receiveYapeEvent(token, payload(key, start));
        assertEquals(event, retry.paymentEventId());
        assertTrue(retry.duplicate());
        assertNull(eventStatus(event));
        assertEquals("WAITING", requestStatus(id));
        assertNull(jdbc.queryForObject("SELECT occurred_at_instant FROM payment_events WHERE id = ?", Timestamp.class, event));
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "device", "company", "branch", "branchCompany"})
    void revokedDeviceAccessCannotInsert(String reason) throws Exception {
        switch (reason) {
            case "device" -> jdbc.update("UPDATE receiver_devices SET active = false WHERE id = ?", deviceId);
            case "company" -> jdbc.update("UPDATE companies SET active = false WHERE id = ?", companyId);
            case "branch" -> jdbc.update("UPDATE branches SET active = false WHERE id = ?", branchId);
            case "branchCompany" -> {
                var other = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Other') RETURNING id", Long.class);
                jdbc.update("UPDATE branches SET company_id = ? WHERE id = ?", other, branchId);
            }
        }
        mvc.perform(post(URL).header("X-Device-Token", reason.equals("token") ? "invalid" : token)
                        .contentType("application/json").content("{\"amount\":80,\"sourceEventId\":\"access-test\",\"occurredAtEpochMillis\":0}"))
                .andExpect(status().is4xxClientError());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ?", Long.class, companyId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "sourceLength", "senderLength", "codeLength", "nul", "maxTime", "minTime"})
    void invalidPayloadReturns400WithoutInsertion(String reason) throws Exception {
        String body = "{\"amount\":" + (reason.equals("amount") ? "80.001" : "80")
                + ",\"sourceEventId\":\"" + (reason.equals("sourceLength") ? "x".repeat(151) : UUID.randomUUID())
                + "\",\"sender\":\"" + (reason.equals("senderLength") ? "x".repeat(101) : reason.equals("nul") ? "a\\u0000b" : "Sender")
                + "\",\"securityCode\":\"" + (reason.equals("codeLength") ? "x".repeat(21) : "123")
                + "\",\"occurredAtEpochMillis\":" + (reason.equals("maxTime") ? Long.MAX_VALUE : reason.equals("minTime") ? Long.MIN_VALUE : start.toEpochMilli()) + "}";
        mvc.perform(post(URL).header("X-Device-Token", token).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_events WHERE company_id = ?", Long.class, companyId));
    }
}
