package com.verifipay.subscription;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionMigrationTest {
    @Autowired DataSource dataSource;

    @Test void v12PreservesExistingDataSeedsPlansAndEnforcesConstraints() throws Exception {
        String schema = "subscription_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().contains("test"));
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            try {
                Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("11").load().migrate();
                connection.setSchema(schema);
                Long company = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Historical') RETURNING id",Long.class);
                Long secondCompany = jdbc.queryForObject("INSERT INTO companies(name) VALUES ('Pending') RETURNING id",Long.class);
                Long branch = jdbc.queryForObject("INSERT INTO branches(company_id,name) VALUES (?,'Central') RETURNING id",Long.class,company);
                Long cashier = jdbc.queryForObject("INSERT INTO users(company_id,branch_id,name,email,password,role) VALUES (?,?,'Cashier','s2@test','x','CASHIER') RETURNING id",Long.class,company,branch);
                jdbc.update("INSERT INTO payment_events(company_id,branch_id,method,amount,occurred_at) VALUES (?,?,'YAPE',40,CURRENT_TIMESTAMP)",company,branch);
                jdbc.update("INSERT INTO payment_requests(company_id,cashier_id,amount,status,client_request_id,created_at,expires_at) VALUES (?,?,40,'WAITING','s2',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP + interval '5 minutes')",company,cashier);
                Map<String,List<Map<String,Object>>> before = new HashMap<>();
                for (String table : List.of("companies","branches","users","payment_events","payment_requests","verifications","receiver_devices","audit_logs","payment_match_candidates"))
                    before.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
                assertEquals(1,Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("12").load().migrate().migrationsExecuted);
                before.forEach((table, rows) -> assertEquals(rows,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id")));
                assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM subscriptions",Long.class));
                assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND table_name='companies' AND column_name='subscription_id'",Long.class,schema));
                var seeded = jdbc.queryForList("SELECT * FROM plans ORDER BY price");
                assertEquals(2,seeded.size());
                for (int i=0;i<2;i++) {
                    var p = seeded.get(i);
                    assertEquals(i==0 ? "BASIC" : "PRO",p.get("code"));
                    assertEquals(new BigDecimal(i==0 ? "40.00" : "80.00"),p.get("price"));
                    assertEquals("PEN",p.get("currency"));
                    assertEquals("MONTHLY",p.get("billing_interval"));
                    assertEquals(i==0 ? 1 : 5,p.get("max_cashiers"));
                    assertEquals(1,p.get("max_receiver_devices"));
                    assertEquals(true,p.get("active"));
                }
                Long plan = ((Number)seeded.getFirst().get("id")).longValue();
                for (String assignment : List.of("price=0","price=-1","max_cashiers=-1","max_receiver_devices=-1","currency='pen'","currency='PE'","billing_interval='YEARLY'","code='PRO'"))
                    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE plans SET "+assignment+" WHERE id=?",plan));

                String insert = "INSERT INTO subscriptions(company_id,plan_id,status,contracted_price,currency,billing_interval,started_at) VALUES (?,?,'PENDING_PAYMENT',40,'PEN','MONTHLY','2026-01-01Z') RETURNING id";
                Long subscription = jdbc.queryForObject(insert,Long.class,company,plan);
                assertThrows(DataIntegrityViolationException.class,() -> jdbc.queryForObject(insert,Long.class,company,plan));
                Long pendingWithoutStart = jdbc.queryForObject(
                        "INSERT INTO subscriptions(company_id,plan_id,status,contracted_price,currency,billing_interval,started_at) "
                        + "VALUES (?,?,'PENDING_PAYMENT',40,'PEN','MONTHLY',NULL) RETURNING id", Long.class, secondCompany, plan);
                assertNull(jdbc.queryForObject("SELECT started_at FROM subscriptions WHERE id=?", java.time.OffsetDateTime.class, pendingWithoutStart));
                assertThrows(DataIntegrityViolationException.class,
                        () -> jdbc.update("UPDATE subscriptions SET ended_at=CURRENT_TIMESTAMP WHERE id=?", pendingWithoutStart));
                assertThrows(DataIntegrityViolationException.class,
                        () -> jdbc.update("UPDATE subscriptions SET cancelled_at=CURRENT_TIMESTAMP WHERE id=?", pendingWithoutStart));
                for (String assignment : List.of("company_id=9223372036854775807","plan_id=9223372036854775807","contracted_price=0","currency='pen'","billing_interval='YEARLY'","status='UNKNOWN'","status='CANCELLED'","status='EXPIRED'","ended_at='2026-01-02Z'","cancelled_at='2025-01-01Z'","status='EXPIRED',ended_at='2025-01-01Z'"))
                    assertThrows(DataIntegrityViolationException.class,() -> jdbc.update("UPDATE subscriptions SET "+assignment+" WHERE id=?",subscription));
                for (String status : List.of("ACTIVE","PAST_DUE","PENDING_PAYMENT"))
                    assertEquals(1,jdbc.update("UPDATE subscriptions SET status=? WHERE id=?",status,subscription));
                jdbc.update("UPDATE subscriptions SET status='CANCELLED',ended_at=started_at,cancelled_at=started_at WHERE id=?",subscription);
                Long next = jdbc.queryForObject(insert,Long.class,company,plan);
                jdbc.update("UPDATE subscriptions SET status='EXPIRED',ended_at=started_at WHERE id=?",next);
                jdbc.queryForObject(insert,Long.class,company,plan);
                assertEquals(3L,jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE company_id=?",Long.class,company));
            } finally {
                connection.setSchema("public");
                jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
            }
        }
    }
}
