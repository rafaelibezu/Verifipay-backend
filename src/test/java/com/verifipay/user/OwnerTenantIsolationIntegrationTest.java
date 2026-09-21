package com.verifipay.user;

import com.verifipay.common.exception.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OwnerTenantIsolationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired OwnerCashierService cashiers;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void ownerCannotReadOrCreateCashierInAnotherCompany() {
        Long first = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('First') RETURNING id", Long.class);
        Long second = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Second') RETURNING id", Long.class);
        Long firstBranch = jdbc.queryForObject("INSERT INTO branches(company_id,name) VALUES (?, 'First') RETURNING id", Long.class, first);
        Long secondBranch = jdbc.queryForObject("INSERT INTO branches(company_id,name) VALUES (?, 'Second') RETURNING id", Long.class, second);
        Long owner = jdbc.queryForObject("INSERT INTO users(company_id,name,email,password,role) VALUES (?, 'Owner', ?, 'x', 'OWNER') RETURNING id", Long.class, first, java.util.UUID.randomUUID()+"@test");
        Long foreignCashier = jdbc.queryForObject("INSERT INTO users(company_id,branch_id,name,email,password,role) VALUES (?, ?, 'Cashier', ?, 'x', 'CASHIER') RETURNING id", Long.class, second, secondBranch, java.util.UUID.randomUUID()+"@test");
        authenticate(owner);
        assertThrows(ResourceNotFoundException.class, () -> cashiers.find(foreignCashier));
        assertThrows(ResourceNotFoundException.class, () -> cashiers.create(new CreateCashierRequest(secondBranch, "New", java.util.UUID.randomUUID()+"@test", "password1")));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM users WHERE company_id = ? AND branch_id = ?", Long.class, first, secondBranch));
        assertNotNull(firstBranch);
    }

    private void authenticate(Long userId) {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").claim("userId", userId).claim("role", "OWNER").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }
}
