package com.verifipay.payment;

import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.service.DevicePaymentEventService;
import com.verifipay.device.util.DeviceTokenUtil;
import com.verifipay.paymentrequest.dto.CreatePaymentRequestRequest;
import com.verifipay.paymentrequest.service.PaymentRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PaymentMatchingTestSupport {
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected DevicePaymentEventService events;
    @Autowired protected PaymentRequestService requests;
    @Autowired protected DeviceTokenUtil tokens;
    @Autowired protected PlatformTransactionManager transactionManager;
    protected Long companyId, branchId, cashierId, deviceId;
    protected String token;
    protected final Instant start = Instant.parse("2026-01-01T19:00:00Z");

    @BeforeEach
    void fixtures() {
        companyId = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Matching test') RETURNING id", Long.class);
        branchId = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, 'Central') RETURNING id",
                Long.class, companyId);
        cashierId = newCashier(branchId);
        token = UUID.randomUUID().toString();
        deviceId = jdbc.queryForObject("""
                INSERT INTO receiver_devices(company_id, branch_id, name, token_hash)
                VALUES (?, ?, 'Test device', ?) RETURNING id
                """, Long.class, companyId, branchId, tokens.hash(token));
    }

    protected Long newCashier(Long branch) {
        return jdbc.queryForObject("""
                INSERT INTO users(company_id, branch_id, name, email, password, role)
                VALUES (?, ?, 'Cashier', ?, 'unused-test-password', 'CASHIER') RETURNING id
                """, Long.class, companyId, branch, UUID.randomUUID() + "@example.test");
    }

    protected Long request(String status, Instant cancelledAt) {
        return jdbc.queryForObject("""
                INSERT INTO payment_requests(company_id, branch_id, cashier_id, amount, status,
                    client_request_id, created_at, expires_at, cancelled_at)
                VALUES (?, ?, ?, 80, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, companyId, branchId, cashierId, status, UUID.randomUUID().toString(),
                Timestamp.from(start), Timestamp.from(start.plusSeconds(300)),
                cancelledAt == null ? null : Timestamp.from(cancelledAt));
    }

    protected YapeDeviceEventRequest payload(String key, Instant occurredAt) {
        return new YapeDeviceEventRequest(new BigDecimal("80.00"), "123", "Sender", occurredAt.toEpochMilli(), key);
    }

    protected Long receive(Instant occurredAt) {
        return events.receiveYapeEvent(token, payload(UUID.randomUUID().toString(), occurredAt)).paymentEventId();
    }

    protected String requestStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM payment_requests WHERE id = ?", String.class, id);
    }

    protected String eventStatus(Long id) {
        return jdbc.queryForObject("SELECT matching_status FROM payment_events WHERE id = ?", String.class, id);
    }

    protected Instant requestTime(Long id, String column) {
        if (!List.of("paid_at", "cancelled_at").contains(column)) throw new IllegalArgumentException();
        return jdbc.queryForObject("SELECT " + column + " FROM payment_requests WHERE id = ?",
                (rs, row) -> rs.getTimestamp(1) == null ? null : rs.getTimestamp(1).toInstant(), id);
    }

    protected <T> T asCashier(Callable<T> action) throws Exception {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("test").header("alg", "none").claim("userId", cashierId).build(),
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER"))));
        SecurityContextHolder.setContext(context);
        try { return action.call(); }
        finally { SecurityContextHolder.clearContext(); }
    }

    protected Long createRequest() throws Exception {
        return asCashier(() -> requests.create(new CreatePaymentRequestRequest(
                new BigDecimal("80"), UUID.randomUUID().toString())).paymentRequest().id());
    }

    protected TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }
}
