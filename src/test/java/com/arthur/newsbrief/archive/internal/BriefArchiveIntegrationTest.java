package com.arthur.newsbrief.archive.internal;

import java.time.Instant;
import java.util.List;

import com.arthur.newsbrief.PostgresBackedTest;
import com.arthur.newsbrief.archive.ArchivedBrief;
import com.arthur.newsbrief.archive.BriefArchive;
import com.arthur.newsbrief.brief.BriefGenerated;
import com.arthur.newsbrief.brief.DailyBrief;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The archive, end to end: an event published by the brief module ends up as a row.
 *
 * <p>Runs against a real Postgres because it exercises Flyway, an identity column and
 * {@code IS NOT DISTINCT FROM} — none of which an in-memory substitute reproduces faithfully.
 */
@SpringBootTest(properties = "newsbrief.news-api.key=test-key")
@ActiveProfiles("test")
@EnabledIf(value = "com.arthur.newsbrief.PostgresBackedTest#databaseIsReachable",
        disabledReason = "Docker unavailable, or its published ports do not forward traffic here")
class BriefArchiveIntegrationTest extends PostgresBackedTest {

    private static final DailyBrief BRIEF = new DailyBrief(
            "Markets steady",
            "A quiet day across most sectors.",
            List.of(new DailyBrief.Topic("Rates held", "The central bank paused.", "business")),
            new DailyBrief.Sources("us", "business", 12, Instant.parse("2026-09-08T07:00:00Z")),
            Instant.parse("2026-09-08T07:00:05Z"));

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private BriefArchive archive;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void anEventPublishedByTheBriefModuleBecomesAnArchivedRow() {
        long before = archive.count();

        // Published inside a transaction: Spring Modulith writes the publication to its log as
        // part of it, which is what makes delivery survivable.
        transactions.executeWithoutResult(status ->
                events.publishEvent(new BriefGenerated("us/business", BRIEF, Instant.parse("2026-09-08T07:00:05Z"))));

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> archive.count() > before);

        ArchivedBrief stored = archive.recent(1).getFirst();
        assertThat(stored.headline()).isEqualTo("Markets steady");
        assertThat(stored.country()).isEqualTo("us");
        assertThat(stored.category()).isEqualTo("business");
        assertThat(stored.topics()).singleElement()
                .satisfies(topic -> assertThat(topic.title()).isEqualTo("Rates held"));
    }

    @Test
    @Transactional
    void readsBackABriefStoredWithoutACategory() {
        DailyBrief noCategory = new DailyBrief(
                "All sectors", "Overview.", List.of(),
                new DailyBrief.Sources("gb", null, 5, Instant.now()), Instant.now());

        transactions.executeWithoutResult(status ->
                events.publishEvent(new BriefGenerated("gb", noCategory, Instant.now())));

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> archive.recent(20).stream().anyMatch(b -> "gb".equals(b.country())));

        // A null category has to be matched with IS NOT DISTINCT FROM; plain equality never
        // matches it, and the lookup would silently return nothing.
        assertThat(archive.forDay(java.time.LocalDate.now(java.time.ZoneOffset.UTC), "gb", null))
                .isPresent();
    }
}
