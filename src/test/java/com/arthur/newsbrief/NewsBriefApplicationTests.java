package com.arthur.newsbrief;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: every bean, property binding and auto-configuration wires up. */
@SpringBootTest(properties = "newsbrief.news-api.key=test-key")
@ActiveProfiles("test")
class NewsBriefApplicationTests {

    @Test
    void contextLoads() {
    }
}
