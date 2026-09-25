package com.verifipay.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PostgresPlatformBootstrapLockIntegrationTest {

    @Autowired
    PlatformBootstrapLock bootstrapLock;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void advisoryTransactionLockRunsInsideRealTransaction() {
        Boolean executedInTransaction = transactionTemplate.execute(status -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            bootstrapLock.lock();
            return TransactionSynchronizationManager.isActualTransactionActive();
        });

        assertThat(executedInTransaction).isTrue();
    }
}
