package com.verifipay.device;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ReceiverDeviceCurrentIntegrationTest {
    private static final String URL = "/api/v1/owner/receiver-devices/current";

    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void returnsOwnersActiveDeviceWithoutCredentials() throws Exception {
        Tenant ownerTenant = tenant("Owner");
        Tenant otherTenant = tenant("Other");
        Long ownerDevice = device(ownerTenant, "Owner receiver", true);
        device(otherTenant, "Other receiver", true);

        mvc.perform(get(URL).with(authentication(jwtAuthentication(ownerTenant.ownerId(), "ROLE_OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownerDevice))
                .andExpect(jsonPath("$.data.name").value("Owner receiver"))
                .andExpect(jsonPath("$.data.branchId").value(ownerTenant.branchId()))
                .andExpect(jsonPath("$.data.deviceToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenHash").doesNotExist());
    }

    @Test
    void returnsNotFoundWhenOwnerHasNoActiveDeviceEvenIfAnotherTenantDoes() throws Exception {
        Tenant ownerTenant = tenant("Owner without receiver");
        Tenant otherTenant = tenant("Other with receiver");
        device(ownerTenant, "Inactive receiver", false);
        device(otherTenant, "Other receiver", true);

        mvc.perform(get(URL).with(authentication(jwtAuthentication(ownerTenant.ownerId(), "ROLE_OWNER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNonOwnerRole() throws Exception {
        Tenant tenant = tenant("Cashier tenant");
        Long cashierId = jdbc.queryForObject(
            "INSERT INTO users(company_id, branch_id, name, email, password, role) VALUES (?, ?, 'Cashier', ?, 'x', 'CASHIER') RETURNING id",
            Long.class, tenant.companyId(), tenant.branchId(), UUID.randomUUID() + "@test");
        device(tenant, "Receiver", true);

        mvc.perform(get(URL).with(authentication(jwtAuthentication(cashierId, "ROLE_CASHIER"))))
                .andExpect(status().isForbidden());
    }

    private Tenant tenant(String name) {
        Long companyId = jdbc.queryForObject("INSERT INTO companies(name) VALUES (?) RETURNING id", Long.class, name + " " + UUID.randomUUID());
        Long branchId = jdbc.queryForObject("INSERT INTO branches(company_id, name) VALUES (?, ?) RETURNING id",
                Long.class, companyId, name + " branch");
        Long ownerId = jdbc.queryForObject(
                "INSERT INTO users(company_id, name, email, password, role) VALUES (?, 'Owner', ?, 'x', 'OWNER') RETURNING id",
                Long.class, companyId, UUID.randomUUID() + "@test");
        return new Tenant(companyId, branchId, ownerId);
    }

    private Long device(Tenant tenant, String name, boolean active) {
        String tokenHash = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        return jdbc.queryForObject(
                "INSERT INTO receiver_devices(company_id, branch_id, name, token_hash, active) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class, tenant.companyId(), tenant.branchId(), name, tokenHash, active);
    }

    private JwtAuthenticationToken jwtAuthentication(Long userId, String role) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").claim("userId", userId).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
    }

    private record Tenant(Long companyId, Long branchId, Long ownerId) { }
}