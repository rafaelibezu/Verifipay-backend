package com.verifipay.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresPlatformBootstrapLock implements PlatformBootstrapLock {

    private static final long LOCK_KEY = 9_011_001L;

    private final EntityManager entityManager;

    @Override
    public void lock() {
        entityManager
                .createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", LOCK_KEY)
                .getSingleResult();
    }
}
