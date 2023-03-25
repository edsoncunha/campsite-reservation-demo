package io.github.edsoncunha.upgrade.takehome.domain.services;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

public interface LockManager {
    @Transactional
    void lock(long id, Duration timeout, Runnable operation);
}
