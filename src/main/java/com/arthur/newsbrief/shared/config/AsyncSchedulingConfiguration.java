package com.arthur.newsbrief.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the two facilities the warm-up job and the archive depend on.
 *
 * <p>{@code @EnableAsync} is not optional here: Spring Modulith's
 * {@code @ApplicationModuleListener} is meta-annotated {@code @Async}, and without async
 * enabled the archive listener would run inline on the request thread.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
class AsyncSchedulingConfiguration {
}
