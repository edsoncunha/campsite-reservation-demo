package io.github.edsoncunha.upgrade.takehome.domain.services;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Supplier;

public interface LockManager {
    @Transactional
    <T> T lock(long id, Duration timeout, Supplier<T> supplier);
}
