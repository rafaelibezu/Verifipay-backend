package com.verifipay.subscription;

import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.device.dto.CreateReceiverDeviceRequest;
import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.service.DevicePaymentEventService;
import com.verifipay.device.service.ReceiverDeviceService;
import com.verifipay.subscription.service.PlanQuotaService;
import com.verifipay.user.dto.CreateCashierRequest;
import com.verifipay.user.service.OwnerCashierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PlanQuotaIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired OwnerCashierService cashiers;
    @Autowired ReceiverDeviceService devices;
    @Autowired DevicePaymentEventService events;
    @Autowired PlanQuotaService quotas;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cashierQuotaUsesActiveResourcesAndReactivation() {
        Tenant tenant = tenantWithCurrentSubscription(1, 1, true);
        authenticate(tenant.ownerId());

        Long first = cashiers.create(cashierRequest(tenant.branchId())).id();
        BusinessException limit = assertThrows(BusinessException.class,
                () -> cashiers.create(cashierRequest(tenant.branchId())));
        assertEquals("El Plan actual permite como máximo 1 cajeros activos", limit.getMessage());

        cashiers.status(first, false);
        cashiers.create(cashierRequest(tenant.branchId()));
        assertThrows(BusinessException.class, () -> cashiers.status(first, true));
        assertEquals(1L, activeCashiers(tenant.companyId()));
    }

    @Test
    void proAllowsFiveCashiersButNotSix() {
        Tenant tenant = tenantWithCurrentSubscription(5, 1, true);
        authenticate(tenant.ownerId());

        for (int i = 0; i < 5; i++) cashiers.create(cashierRequest(tenant.branchId()));

        BusinessException limit = assertThrows(BusinessException.class,
                () -> cashiers.create(cashierRequest(tenant.branchId())));
        assertEquals("El Plan actual permite como máximo 5 cajeros activos", limit.getMessage());
    }

    @Test
    void deviceQuotaUsesCurrentSubscriptionEvenWhenPlanIsInactive() {
        Tenant tenant = tenantWithCurrentSubscription(1, 1, false);
        authenticate(tenant.ownerId());

        Long first = devices.create(deviceRequest(tenant.branchId())).id();
        assertThrows(BusinessException.class, () -> devices.create(deviceRequest(tenant.branchId())));

        devices.deactivate(first);
        devices.create(deviceRequest(tenant.branchId()));
        assertEquals(1L, activeDevices(tenant.companyId()));
    }

    @Test
    void quotaServiceRequiresTheCallersTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> quotas.assertCanActivateCashier(1L));
    }

    @Test
    void ownerCanDeactivateDeviceAndItCanNoLongerAuthenticateEvents() {
        Tenant tenant = tenantWithCurrentSubscription(1, 1, true);
        authenticate(tenant.ownerId());
        var device = devices.create(deviceRequest(tenant.branchId()));

        devices.deactivate(device.id());

        assertEquals(0L, activeDevices(tenant.companyId()));
        assertEquals(false, jdbc.queryForObject("SELECT active FROM receiver_devices WHERE id = ?", Boolean.class, device.id()));
        assertEquals("Dispositivo no autorizado", assertThrows(BusinessException.class,
                () -> events.receiveYapeEvent(device.deviceToken(), new YapeDeviceEventRequest(new BigDecimal("80"), "123", "Sender",
                        Instant.now().toEpochMilli(), UUID.randomUUID().toString()))).getMessage());
    }

    @Test
    void ownerCannotDeactivateAnotherCompanysDevice() {
        Tenant first = tenantWithCurrentSubscription(1, 1, true);
        Tenant second = tenantWithCurrentSubscription(1, 1, true);
        authenticate(first.ownerId());
        Long device = devices.create(deviceRequest(first.branchId())).id();

        authenticate(second.ownerId());
        assertThrows(ResourceNotFoundException.class, () -> devices.deactivate(device));
        assertEquals(1L, activeDevices(first.companyId()));
    }

    @Test
    void missingSubscriptionBlocksActivationWithoutChangingExistingResources() {
        Tenant tenant = tenantWithoutSubscription();
        authenticate(tenant.ownerId());
        Long existing = jdbc.queryForObject("INSERT INTO users(company_id,branch_id,name,email,password,role) VALUES (?,?,'Existing',?,'x','CASHIER') RETURNING id",
                Long.class, tenant.companyId(), tenant.branchId(), UUID.randomUUID() + "@test");
        jdbc.update("UPDATE users SET active = false WHERE id = ?", existing);

        assertEquals("La empresa no tiene una Subscription vigente", assertThrows(BusinessException.class,
                () -> cashiers.create(cashierRequest(tenant.branchId()))).getMessage());
        assertThrows(BusinessException.class, () -> cashiers.status(existing, true));
        assertEquals(0L, activeCashiers(tenant.companyId()));
    }

    @Test
    void quotasAreIsolatedByCompany() {
        Tenant first = tenantWithCurrentSubscription(1, 1, true);
        Tenant second = tenantWithCurrentSubscription(1, 1, true);
        authenticate(first.ownerId());
        cashiers.create(cashierRequest(first.branchId()));
        authenticate(second.ownerId());
        cashiers.create(cashierRequest(second.branchId()));
        assertEquals(1L, activeCashiers(first.companyId()));
        assertEquals(1L, activeCashiers(second.companyId()));
    }

    @Test
    void concurrentCashierCreationLeavesOnlyOneActiveAtTheLimit() throws Exception {
        Tenant tenant = tenantWithCurrentSubscription(1, 1, true);
        assertOneConcurrentSuccess(tenant, () -> cashiers.create(cashierRequest(tenant.branchId())));
        assertEquals(1L, activeCashiers(tenant.companyId()));
    }

    @Test
    void concurrentDeviceCreationLeavesOnlyOneActiveAtTheLimit() throws Exception {
        Tenant tenant = tenantWithCurrentSubscription(1, 1, true);
        assertOneConcurrentSuccess(tenant, () -> devices.create(deviceRequest(tenant.branchId())));
        assertEquals(1L, activeDevices(tenant.companyId()));
    }

    private void assertOneConcurrentSuccess(Tenant tenant, Callable<?> action) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt = () -> {
                authenticate(tenant.ownerId());
                try {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    action.call();
                    return true;
                } catch (BusinessException expected) {
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    private Tenant tenantWithCurrentSubscription(int maxCashiers, int maxDevices, boolean planActive) {
        Tenant tenant = tenantWithoutSubscription();
        Long plan = jdbc.queryForObject("INSERT INTO plans(code,name,price,currency,billing_interval,max_cashiers,max_receiver_devices,active) VALUES (?,?,40,'PEN','MONTHLY',?,?,?) RETURNING id",
                Long.class, "QUOTA_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), "Quota", maxCashiers, maxDevices, planActive);
        jdbc.update("INSERT INTO subscriptions(company_id,plan_id,status,contracted_price,currency,billing_interval) VALUES (?,?,'PENDING_PAYMENT',40,'PEN','MONTHLY')",
                tenant.companyId(), plan);
        return tenant;
    }

    private Tenant tenantWithoutSubscription() {
        Long company = jdbc.queryForObject("INSERT INTO companies(name) VALUES (?) RETURNING id", Long.class, "Quota " + UUID.randomUUID());
        Long branch = jdbc.queryForObject("INSERT INTO branches(company_id,name) VALUES (?,?) RETURNING id", Long.class, company, "Branch " + UUID.randomUUID());
        Long owner = jdbc.queryForObject("INSERT INTO users(company_id,name,email,password,role) VALUES (?, 'Owner', ?, 'x', 'OWNER') RETURNING id",
                Long.class, company, UUID.randomUUID() + "@test");
        return new Tenant(company, branch, owner);
    }

    private CreateCashierRequest cashierRequest(Long branchId) {
        return new CreateCashierRequest(branchId, "Cashier", UUID.randomUUID() + "@test", "password1");
    }

    private CreateReceiverDeviceRequest deviceRequest(Long branchId) {
        return new CreateReceiverDeviceRequest(branchId, "Device " + UUID.randomUUID());
    }

    private long activeCashiers(Long companyId) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE company_id = ? AND role = 'CASHIER' AND active", Long.class, companyId);
    }

    private long activeDevices(Long companyId) {
        return jdbc.queryForObject("SELECT count(*) FROM receiver_devices WHERE company_id = ? AND active", Long.class, companyId);
    }

    private void authenticate(Long userId) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("userId", userId).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }

    private record Tenant(Long companyId, Long branchId, Long ownerId) { }
}
