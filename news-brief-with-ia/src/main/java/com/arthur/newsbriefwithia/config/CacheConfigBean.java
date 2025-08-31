package com.arthur.newsbriefwithia.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfigBean {

    @Bean
    public CacheManager cacheManager() {

        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(2 , TimeUnit.MINUTES)
                .maximumSize(100)
        );

        return caffeineCacheManager;
    }
}
