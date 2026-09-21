package com.verifipay.subscription;

import com.verifipay.common.enums.BillingInterval;
import com.verifipay.common.enums.SubscriptionStatus;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.plan.dto.*;
import com.verifipay.plan.service.PlanService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SubscriptionService subscriptions;
    @Autowired PlanService plans;
    @Autowired WebApplicationContext context;
    Long admin;
    Long company;
    Long plan;
    MockMvc mvc;

    @BeforeEach void setup() {
        company = newCompany();
        admin = jdbc.queryForObject("INSERT INTO users(name,email,password,role) VALUES ('Admin',?,'x','PLATFORM_ADMIN') RETURNING id",
                Long.class, UUID.randomUUID()+"@test");
        authenticate();
        plan = plans.create(new CreatePlanRequest("TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10), "Test",
                new BigDecimal("40.00"), "PEN", BillingInterval.MONTHLY, 1, 1, true)).id();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    Long newCompany() { return jdbc.queryForObject("INSERT INTO companies(name) VALUES ('S2 test') RETURNING id", Long.class); }
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("test").header("alg","none").claim("userId",admin).build(),
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))));
    }

    @Test void snapshotIsIndependentAndCompaniesCanHaveSeparateCurrentSubscriptions() {
        var first = subscriptions.create(company, new CreateSubscriptionRequest(plan));
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, first.status());
        assertEquals(new BigDecimal("40.00"), first.contractedPrice());
        assertEquals("PEN", first.currency());
        assertEquals(BillingInterval.MONTHLY, first.billingInterval());
        assertEquals(0L, first.version());
        assertNull(first.startedAt());
        assertNull(first.endedAt());
        assertNull(first.cancelledAt());
        assertEquals(first.id(), subscriptions.current(company).id());
        plans.update(plan, new UpdatePlanRequest(null,new BigDecimal("80.00"),"USD",null,5,null,null));
        var current = subscriptions.current(company);
        assertEquals(first.contractedPrice(), current.contractedPrice());
        assertEquals("PEN", current.currency());
        assertEquals(new BigDecimal("80.00"), subscriptions.create(newCompany(),new CreateSubscriptionRequest(plan)).contractedPrice());
        assertThrows(BusinessException.class, () -> subscriptions.create(company,new CreateSubscriptionRequest(plan)));
        assertEquals(1, subscriptions.history(company).size());
    }

    @Test void concurrentCreationHasExactlyOneWinner() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> action = () -> {
                authenticate();
                try {
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    subscriptions.create(company,new CreateSubscriptionRequest(plan));
                    return true;
                } catch (BusinessException expected) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            };
            var first = executor.submit(action);
            var second = executor.submit(action);
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE company_id=? AND ended_at IS NULL",Long.class,company));
    }

    @Test void inactiveOrMissingPlanCannotBeSubscribed() {
        plans.update(plan,new UpdatePlanRequest(null,null,null,null,null,null,false));
        assertThrows(BusinessException.class, () -> subscriptions.create(company,new CreateSubscriptionRequest(plan)));
        assertThrows(com.verifipay.common.exception.ResourceNotFoundException.class,
                () -> subscriptions.create(company,new CreateSubscriptionRequest(Long.MAX_VALUE)));
        assertTrue(subscriptions.history(company).isEmpty());
    }

    @Test void platformRoutesEnforceRolesAndRejectContractOverrides() throws Exception {
        String path = "/api/v1/platform/companies/"+company+"/subscriptions";
        for (String role : List.of("OWNER","CASHIER")) {
            var auth = jwt().authorities(new SimpleGrantedAuthority("ROLE_"+role));
            for (String route : List.of("/api/v1/platform/plans","/api/v1/platform/plans/"+plan,path,path+"/current"))
                mvc.perform(get(route).with(auth)).andExpect(status().isForbidden());
            mvc.perform(post(path).with(auth).contentType("application/json").content("{\"planId\":"+plan+"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/platform/plans").with(auth).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/platform/plans/"+plan).with(auth).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        var auth = jwt().jwt(j -> j.claim("userId",admin)).authorities(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        mvc.perform(get("/api/v1/platform/companies/"+company).with(auth)).andExpect(status().isOk());
        mvc.perform(get(path).with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get(path+"/current").with(auth)).andExpect(status().isNotFound());
        for (String field : List.of("\"status\":\"ACTIVE\"","\"contractedPrice\":1","\"currency\":\"USD\"","\"billingInterval\":\"MONTHLY\""))
            mvc.perform(post(path).with(auth).contentType("application/json")
                    .content("{\"planId\":"+plan+","+field+"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/platform/plans/"+plan).with(auth).contentType("application/json")
                .content("{\"code\":\"CHANGED\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(path).with(auth).contentType("application/json").content("{\"planId\":"+plan+"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
        mvc.perform(get(path+"/current").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/platform/plans").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/platform/plans/"+plan).with(auth)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/platform/plans/"+plan).with(auth).contentType("application/json")
                .content("{\"price\":60}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/platform/plans").with(auth).contentType("application/json")
                .content("{\"code\":\"TEST_"+UUID.randomUUID().toString().replace("-", "").substring(0,15).toUpperCase()+"\",\"name\":\"Test\",\"price\":40,\"currency\":\"PEN\",\"billingInterval\":\"MONTHLY\",\"maxCashiers\":1,\"maxReceiverDevices\":1,\"active\":true}"))
                .andExpect(status().isCreated());
    }
}
