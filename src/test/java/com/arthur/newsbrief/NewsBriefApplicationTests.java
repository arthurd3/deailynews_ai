package com.arthur.newsbrief;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: every bean, property binding and auto-configuration wires up. */
@EnabledIf(value = "com.arthur.newsbrief.PostgresBackedTest#databaseIsReachable",
        disabledReason = "Docker unavailable, or its published ports do not forward traffic here")
@SpringBootTest(properties = "newsbrief.news-api.key=test-key")
@ActiveProfiles("test")
class NewsBriefApplicationTests extends PostgresBackedTest {

    @Test
    void contextLoads() {
    }
}
