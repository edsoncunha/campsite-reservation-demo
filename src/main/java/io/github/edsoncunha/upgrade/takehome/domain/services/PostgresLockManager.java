package io.github.edsoncunha.upgrade.takehome.domain.services;

import io.github.edsoncunha.upgrade.takehome.domain.exceptions.LockNotAcquiredException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
public class PostgresLockManager implements LockManager {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public <T> T lock(long id, Duration timeout, Supplier<T> supplier) {
        acquireLock(id, timeout);
        // only runs if lock is acquired, so we have a critical section below this line
        return supplier.get();
    }

    private void acquireLock(long id, Duration timeout) {
        RetryTemplate retryTemplate
                = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(100, 2, timeout.toMillis(), true)
                .retryOn(LockNotAcquiredException.class)
                .traversingCauses()
                .build();

        retryTemplate.execute(retryContext -> {
            boolean acquired = Boolean.TRUE.equals(jdbcTemplate
                    .queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, id));

            if (!acquired) {
                throw new LockNotAcquiredException("Advisory lock not acquired");
            }
            return null;
        });
    }
}
