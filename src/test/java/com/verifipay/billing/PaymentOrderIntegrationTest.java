package com.verifipay.billing;

import com.verifipay.billing.service.PaymentOrderService;
import com.verifipay.common.enums.*;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.subscription.dto.CreateSubscriptionRequest;
import com.verifipay.subscription.service.SubscriptionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class PaymentOrderIntegrationTest {
    @Autowired JdbcTemplate jdbc; @Autowired SubscriptionService subscriptions; @Autowired PaymentOrderService orders;
    Long admin, company, plan;
    @BeforeEach void setup() {
        company = jdbc.queryForObject("INSERT INTO companies(name) VALUES (?) RETURNING id", Long.class, "Billing "+UUID.randomUUID());
        admin = jdbc.queryForObject("INSERT INTO users(name,email,password,role) VALUES ('Admin',?,'x','PLATFORM_ADMIN') RETURNING id", Long.class, UUID.randomUUID()+"@test");
        plan = jdbc.queryForObject("SELECT id FROM plans WHERE code='BASIC'", Long.class); authenticate();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void createsIdempotentlySnapshotsSubscriptionAndConfirmsAtomically() {
        var subscription = subscriptions.create(company, new CreateSubscriptionRequest(plan));
        var first = orders.create(company, subscription.id(), "key-1");
        var replay = orders.create(company, subscription.id(), "key-1");
        assertEquals(first.id(), replay.id()); assertEquals(subscription.contractedPrice(), first.amount()); assertEquals(subscription.currency(), first.currency());
        var paid = orders.confirmPaid(company, subscription.id(), first.id());
        assertEquals(PaymentOrderStatus.PAID, paid.status()); assertNotNull(paid.paidAt());
        var active = subscriptions.current(company); assertEquals(SubscriptionStatus.ACTIVE, active.status()); assertNotNull(active.startedAt());
        assertTrue(java.time.Duration.between(paid.paidAt(), orders.confirmPaid(company, subscription.id(), first.id()).paidAt()).abs().compareTo(java.time.Duration.ofNanos(1_000)) <= 0);
    }
    @Test void rejectsInvalidStatesAndPendingDuplicates() {
        var subscription = subscriptions.create(company, new CreateSubscriptionRequest(plan));
        var order = orders.create(company, subscription.id(), "key-1");
        assertThrows(BusinessException.class, () -> orders.create(company, subscription.id(), "key-2"));
        subscriptions.activate(company, subscription.id());
        assertThrows(BusinessException.class, () -> orders.confirmPaid(company, subscription.id(), order.id()));
        assertThrows(BusinessException.class, () -> orders.create(company, subscription.id(), "key-3"));
    }
    @Test void validatesIdempotencyKeyLengthBeforeCreatingAnOrder() {
        var subscription = subscriptions.create(company, new CreateSubscriptionRequest(plan));

        assertThrows(BusinessException.class, () -> orders.create(company, subscription.id(), "   "));
        assertThrows(BusinessException.class, () -> orders.create(company, subscription.id(), "x".repeat(101)));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM payment_orders WHERE subscription_id=?", Long.class, subscription.id()));

        assertDoesNotThrow(() -> orders.create(company, subscription.id(), "x".repeat(100)));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM payment_orders WHERE subscription_id=?", Long.class, subscription.id()));
    }
    @Test void concurrentCreatesLeaveOnePendingOrder() throws Exception {
        var subscription = subscriptions.create(company, new CreateSubscriptionRequest(plan)); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> action = () -> { authenticate(); try { start.await(); orders.create(company, subscription.id(), UUID.randomUUID().toString()); return true; } catch (BusinessException e) { return false; } finally { SecurityContextHolder.clearContext(); } };
            var a=pool.submit(action); var b=pool.submit(action); start.countDown(); assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
        }
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM payment_orders WHERE subscription_id=? AND status='PENDING'",Long.class,subscription.id()));
    }
    void authenticate(){ SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("t").header("alg","none").claim("userId",admin).build(), List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")))); }
}
