package io.github.edsoncunha.upgrade.takehome.configuration;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.edsoncunha.upgrade.takehome.domain.services.ReservationService.AVAILABILITY_SEARCH_CACHE_NAME;


@Component
public class CacheConfiguration implements CacheManagerCustomizer<ConcurrentMapCacheManager> {
    @Override
    public void customize(ConcurrentMapCacheManager cacheManager) {
        cacheManager.setCacheNames(List.of(AVAILABILITY_SEARCH_CACHE_NAME));
    }
}