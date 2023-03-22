package io.github.edsoncunha.upgrade.takehome.domain.exceptions;

public class LockNotAcquiredException extends RuntimeException {
    public LockNotAcquiredException(String message) {
        super(message);
    }
}