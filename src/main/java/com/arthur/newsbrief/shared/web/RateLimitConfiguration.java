package com.arthur.newsbrief.shared.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the per-client rate limiter.
 *
 * <p>Registered from a configuration class rather than annotated {@code @Component} on the
 * filter itself. A stereotyped {@code Filter} is pulled into every {@code @WebMvcTest} slice,
 * where its {@code @ConfigurationProperties} bean does not exist — so controller tests would
 * fail on a dependency they have no interest in. This way the limiter belongs to the full
 * application context, and slice tests stay about controllers.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(name = "newsbrief.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
class RateLimitConfiguration {

    @Bean
    FilterRegistrationBean<ClientRateLimitFilter> clientRateLimitFilter(RateLimitProperties properties) {
        var registration = new FilterRegistrationBean<>(new ClientRateLimitFilter(properties));
        // Ahead of the application, behind the servlet container's own concerns: a rejected
        // request should cost as little as possible.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
