package com.arthur.newsbrief.news.internal;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers {@link NewsApiClient} as an HTTP service proxy and configures the
 * {@code RestClient} behind it.
 *
 * <p>Two things are deliberate here. The API key travels as the {@code X-Api-Key} header
 * rather than a query parameter, so it cannot leak through access logs or a stray URL
 * log statement. And timeouts are set explicitly: an unbounded client will hold a
 * request thread for as long as the upstream cares to stall.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = NewsApiConfiguration.GROUP, types = NewsApiClient.class)
class NewsApiConfiguration {

    static final String GROUP = "newsapi";

    private static final String API_KEY_HEADER = "X-Api-Key";

    @Bean
    RestClientHttpServiceGroupConfigurer newsApiClientConfigurer(NewsApiProperties properties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(properties.connectTimeout(), properties.readTimeout());

        return groups -> groups.filterByName(GROUP).forEachClient((group, builder) -> builder
                .baseUrl(properties.baseUrl().toString())
                .defaultHeader(API_KEY_HEADER, properties.key())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings)));
    }
}
