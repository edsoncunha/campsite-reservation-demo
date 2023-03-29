package io.github.edsoncunha.upgrade.takehome.etc;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Clock {

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
