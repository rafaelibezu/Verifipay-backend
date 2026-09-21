package com.verifipay.paymentrequest;

import com.verifipay.common.enums.PaymentRequestStatus;
import com.verifipay.paymentrequest.dto.CreatePaymentRequestRequest;
import com.verifipay.paymentrequest.service.PaymentRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentRequestIntegrationTest {
    private static final String URL = "/api/v1/payment-requests";
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PaymentRequestService service;
    MockMvc mvc;
    Long companyId;
    Long branchId;
    Long cashierId;
    Long otherCashierId;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // Each test owns its fixtures; no deletion of existing or legacy records.
        companyId = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('PaymentRequest test') RETURNING id", Long.class);
        branchId = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Test branch') RETURNING id", Long.class, companyId);
        cashierId = newCashier(companyId, branchId);
        otherCashierId = newCashier(companyId, branchId);
    }

    Long newCashier(Long company, Long branch) {
        return jdbc.queryForObject("""
                INSERT INTO users(company_id, branch_id, name, email, password, role)
                VALUES (?, ?, 'Test cashier', ?, 'unused-test-password', 'CASHIER') RETURNING id
                """, Long.class, company, branch, java.util.UUID.randomUUID() + "@example.test");
    }

    RequestPostProcessor as(Long id) {
        return jwt().jwt(j -> j.claim("userId", id).claim("companyId", -999L).claim("branchId", -999L))
                .authorities(new SimpleGrantedAuthority("ROLE_CASHIER"));
    }

    <T> T authenticated(Long id, Callable<T> action) throws Exception {
        var token = Jwt.withTokenValue("test").header("alg", "none").claim("userId", id).build();
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
        SecurityContextHolder.setContext(security);
        try { return action.call(); }
        finally { SecurityContextHolder.clearContext(); }
    }

    PaymentRequestService.CreationResult create(Long id, String key, String amount) throws Exception {
        return authenticated(id, () -> service.create(new CreatePaymentRequestRequest(new BigDecimal(amount), key)));
    }

    @Test
    void createsFromDatabaseIdentityAndReplaysWithoutChangingDates() throws Exception {
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":80,\"clientRequestId\":\"mobile-1\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.cashierId").value(cashierId))
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andExpect(jsonPath("$.data.branchId").value(branchId))
                .andExpect(jsonPath("$.data.paidAt").isEmpty());
        var first = create(cashierId, "mobile-1", "80.00");
        assertFalse(first.created());
        assertEquals(Duration.ofMinutes(5), Duration.between(first.paymentRequest().createdAt(), first.paymentRequest().expiresAt()));
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":80.00,\"clientRequestId\":\"mobile-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(first.paymentRequest().id()));
        assertEquals(first.paymentRequest(), create(cashierId, "mobile-1", "80").paymentRequest());
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":81,\"clientRequestId\":\"mobile-1\"}"))
                .andExpect(status().isConflict());
        assertNotEquals(first.paymentRequest().id(), create(otherCashierId, "mobile-1", "80").paymentRequest().id());
    }

    @Test
    void nullableBranchAndAmountBoundaries() throws Exception {
        assertEquals(new BigDecimal("0.01"), create(cashierId, "small", "0.01").paymentRequest().amount());
        assertEquals(new BigDecimal("9999999999.99"), create(cashierId, "large", "9999999999.99").paymentRequest().amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-0.01", "1.001", "10000000000", "0.001"})
    void rejectsInvalidAmounts(String amount) throws Exception {
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":" + amount + ",\"clientRequestId\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_requests WHERE cashier_id = ?", Long.class, cashierId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cashierId", "companyId", "branchId", "status", "extra"})
    void rejectsUnknownFields(String field) throws Exception {
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":80,\"clientRequestId\":\"invalid\",\"" + field + "\":\"1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidIdentifiersAndMalformedRequests() throws Exception {
        for (String key : List.of("null", "\"\"", "\"   \"", "\"" + "a".repeat(151) + "\"")) {
            mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                            .content("{\"amount\":80,\"clientRequestId\":" + key + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL + "/not-an-id").with(as(cashierId))).andExpect(status().isBadRequest());
        mvc.perform(get(URL).param("size", "101").with(as(cashierId))).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPaginationOffsetBeyondJpaLimit() throws Exception {
        mvc.perform(get(URL).param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("size", "20").with(as(cashierId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsClientRequestIdContainingNulBeforePersistence() throws Exception {
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":80,\"clientRequestId\":\"abc\\u0000def\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.clientRequestId").exists());
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM payment_requests WHERE cashier_id = ?", Long.class, cashierId));
    }

    @Test
    void onlyListsReadsAndCancelsOwnedRequests() throws Exception {
        var own = create(cashierId, "own", "20").paymentRequest();
        var other = create(otherCashierId, "other", "20").paymentRequest();
        mvc.perform(get(URL).with(as(cashierId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(own.id()));
        mvc.perform(get(URL + "/" + own.id()).with(as(cashierId))).andExpect(status().isOk());
        for (long id : new long[]{other.id(), Long.MAX_VALUE}) {
            mvc.perform(get(URL + "/" + id).with(as(cashierId))).andExpect(status().isNotFound());
            mvc.perform(post(URL + "/" + id + "/cancel").with(as(cashierId))).andExpect(status().isNotFound());
        }
        mvc.perform(post(URL + "/" + own.id() + "/cancel").with(as(cashierId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertEquals(PaymentRequestStatus.CANCELLED, create(cashierId, "own", "20").paymentRequest().status());
    }

    @Test
    void rejectsOtherCompanyEvenWhenCashierIdMatches() throws Exception {
        var own = create(cashierId, "own", "20").paymentRequest();
        var another = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Another test company') RETURNING id", Long.class);
        var foreignBranch = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Foreign') RETURNING id", Long.class, another);
        jdbc.update("UPDATE users SET company_id = ?, branch_id = ? WHERE id = ?", another, foreignBranch, cashierId);
        mvc.perform(get(URL + "/" + own.id()).with(as(cashierId))).andExpect(status().isNotFound());
        mvc.perform(post(URL + "/" + own.id() + "/cancel").with(as(cashierId))).andExpect(status().isNotFound());
        mvc.perform(get(URL).with(as(cashierId))).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PAID", "AMBIGUOUS", "EXPIRED", "CANCELLED"})
    void cannotCancelNonWaitingRequests(String state) throws Exception {
        var own = create(cashierId, "state", "20").paymentRequest();
        jdbc.update("UPDATE payment_requests SET status = ? WHERE id = ?", state, own.id());
        mvc.perform(post(URL + "/" + own.id() + "/cancel").with(as(cashierId))).andExpect(status().isConflict());
        assertEquals(state, jdbc.queryForObject("SELECT status FROM payment_requests WHERE id = ?", String.class, own.id()));
    }

    @Test
    void elapsedExpirationDoesNotChangeWaitingAndStillAllowsCancellation() throws Exception {
        var own = create(cashierId, "elapsed", "20").paymentRequest();
        jdbc.update("UPDATE payment_requests SET created_at = now() - interval '10 minutes', expires_at = now() - interval '5 minutes' WHERE id = ?", own.id());
        mvc.perform(get(URL + "/" + own.id()).with(as(cashierId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WAITING"));
        var replay = create(cashierId, "elapsed", "20").paymentRequest();
        assertTrue(replay.expiresAt().isBefore(java.time.Instant.now()));
        mvc.perform(post(URL + "/" + own.id() + "/cancel").with(as(cashierId))).andExpect(status().isOk());
    }

    @Test
    void requiresCashierAuthenticationOnEveryEndpoint() throws Exception {
        for (var builder : List.of(get(URL), get(URL + "/1"), post(URL), post(URL + "/1/cancel"))) {
            mvc.perform(builder).andExpect(status().isUnauthorized());
        }
        for (var builder : List.of(get(URL), get(URL + "/1"), post(URL), post(URL + "/1/cancel"))) {
            mvc.perform(builder.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OWNER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "company", "branch", "role"})
    void rejectsRevokedAccessDespiteExistingCashierJwt(String reason) throws Exception {
        switch (reason) {
            case "user" -> jdbc.update("UPDATE users SET active = false WHERE id = ?", cashierId);
            case "company" -> jdbc.update("UPDATE companies SET active = false WHERE id = ?", companyId);
            case "branch" -> jdbc.update("UPDATE branches SET active = false WHERE id = ?", branchId);
            case "role" -> jdbc.update("UPDATE users SET role = 'OWNER' WHERE id = ?", cashierId);
        }
        mvc.perform(get(URL).with(as(cashierId))).andExpect(status().isForbidden());
        mvc.perform(get(URL + "/1").with(as(cashierId))).andExpect(status().isForbidden());
        mvc.perform(post(URL + "/1/cancel").with(as(cashierId))).andExpect(status().isForbidden());
        mvc.perform(post(URL).with(as(cashierId)).contentType("application/json")
                        .content("{\"amount\":80,\"clientRequestId\":\"forbidden\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentRetriesCreateExactlyOneRequest() throws Exception {
        var barrier = new CyclicBarrier(6);
        try (var executor = Executors.newFixedThreadPool(6)) {
            var tasks = java.util.stream.IntStream.range(0, 6)
                    .<Callable<PaymentRequestService.CreationResult>>mapToObj(i -> () -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        return create(cashierId, "concurrent", "80.00");
                    }).toList();
            var results = executor.invokeAll(tasks, 20, TimeUnit.SECONDS);
            var values = new java.util.ArrayList<PaymentRequestService.CreationResult>();
            for (var result : results) values.add(result.get());
            assertEquals(1, values.stream().filter(PaymentRequestService.CreationResult::created).count());
            assertEquals(1, values.stream().map(r -> r.paymentRequest().id()).distinct().count());
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_requests WHERE cashier_id = ?", Long.class, cashierId));
    }

    @Test
    void concurrentDifferentAmountsConflict() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.<Callable<Integer>>of(
                    () -> concurrentCreate(barrier, "80"), () -> concurrentCreate(barrier, "81")), 20, TimeUnit.SECONDS);
            assertEquals(List.of(201, 409), results.stream().map(f -> {
                try { return f.get(); } catch (Exception e) { throw new RuntimeException(e); }
            }).sorted().toList());
        }
    }

    int concurrentCreate(CyclicBarrier barrier, String amount) throws Exception {
        barrier.await(10, TimeUnit.SECONDS);
        try { return create(cashierId, "conflicting", amount).created() ? 201 : 200; }
        catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }

    @Test
    void concurrentCancellationHasOneWinner() throws Exception {
        var own = create(cashierId, "cancel-race", "20").paymentRequest();
        var barrier = new CyclicBarrier(2);
        Callable<Integer> cancel = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try { authenticated(cashierId, () -> service.cancel(own.id())); return 200; }
            catch (ResponseStatusException e) { return e.getStatusCode().value(); }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(cancel, cancel), 20, TimeUnit.SECONDS);
            assertEquals(List.of(200, 409), results.stream().map(f -> {
                try { return f.get(); } catch (Exception e) { throw new RuntimeException(e); }
            }).sorted().toList());
        }
    }

    @Test
    void databaseEnforcesConstraintsAndLegacyTablesRemainUntouched() throws Exception {
        long events = jdbc.queryForObject("SELECT count(*) FROM payment_events", Long.class);
        long verifications = jdbc.queryForObject("SELECT count(*) FROM verifications", Long.class);
        var own = create(cashierId, "constraints", "20").paymentRequest();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO payment_requests(company_id, cashier_id, amount, client_request_id, created_at, expires_at)
                SELECT company_id, cashier_id, amount, client_request_id, created_at, expires_at
                FROM payment_requests WHERE id = ?
                """, own.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE payment_requests SET amount = 0 WHERE id = ?", own.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE payment_requests SET status = 'INVALID' WHERE id = ?", own.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE payment_requests SET expires_at = created_at WHERE id = ?", own.id()));
        authenticated(cashierId, () -> service.cancel(own.id()));
        assertEquals(events, jdbc.queryForObject("SELECT count(*) FROM payment_events", Long.class));
        assertEquals(verifications, jdbc.queryForObject("SELECT count(*) FROM verifications", Long.class));
    }
}
