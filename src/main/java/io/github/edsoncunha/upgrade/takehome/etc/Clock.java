package io.github.edsoncunha.upgrade.takehome.etc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class Clock {
    private String zoneId;

    public Clock(@Value("${campsite.timeZone}") String zoneId) {
        this.zoneId = zoneId;
    }

    public ZonedDateTime campsiteDateTime() {
        return ZonedDateTime.now(getZoneId());
    }

    public ZoneId getZoneId() {
        return ZoneId.of(zoneId);
    }

    public ZonedDateTime asZonedDateTime(LocalDate localDate) {
        return localDate.atStartOfDay(getZoneId());
    }
}
