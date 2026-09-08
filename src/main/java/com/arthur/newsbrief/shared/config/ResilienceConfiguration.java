package com.arthur.newsbrief.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Activates Spring Framework's built-in {@code @Retryable} and {@code @ConcurrencyLimit}
 * interceptors.
 *
 * <p>Retries are proxy-based, so an annotated method only retries when it is called
 * across a bean boundary. Every retried call in this application crosses one: the
 * orchestrating service calls the adapters, never itself.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
class ResilienceConfiguration {
}
