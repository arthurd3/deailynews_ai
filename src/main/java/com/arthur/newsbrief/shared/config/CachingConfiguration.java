package com.arthur.newsbrief.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds a {@link CaffeineCacheManager} whose caches can be tuned independently.
 *
 * <p>Headlines and generated briefs have very different economics — a headline fetch is
 * one cheap HTTP call, a brief is several seconds of local inference — so they must not
 * share a single TTL the way the original single-spec cache manager forced them to.
 *
 * <p>Caching is ordered ahead of the circuit-breaker advice on purpose. With the default
 * ordering a cache hit still travels through the breaker, so an open circuit would
 * suppress results the application already holds — the opposite of what a cache is for
 * during an outage. {@code HeadlinesCircuitBreakerTest} pins that behaviour.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
class CachingConfiguration {

    @Bean
    CacheManager cacheManager(CachingProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.from(properties.defaultSpec()));

        properties.caches().forEach((name, spec) ->
                cacheManager.registerCustomCache(name, Caffeine.from(spec).build()));

        return cacheManager;
    }
}
