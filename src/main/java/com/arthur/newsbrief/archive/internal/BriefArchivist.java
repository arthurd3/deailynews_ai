package com.arthur.newsbrief.archive.internal;

import java.time.ZoneOffset;

import com.arthur.newsbrief.brief.BriefGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Stores every generated brief.
 *
 * <p>{@link ApplicationModuleListener} is transactional and asynchronous, and Spring Modulith
 * writes the publication to its event log inside the publishing transaction. If this listener
 * never completes — the process dies mid-write, the database is briefly unreachable — the
 * publication stays incomplete and is retried on the next start, instead of being lost.
 *
 * <p>Asynchronous also means archiving never slows a request down.
 */
@Component
class BriefArchivist {

    private static final Logger log = LoggerFactory.getLogger(BriefArchivist.class);

    private final ArchivedBriefRepository repository;
    private final ObjectMapper objectMapper;

    BriefArchivist(ArchivedBriefRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @ApplicationModuleListener
    void on(BriefGenerated event) {
        var brief = event.brief();
        var sources = brief.sources();

        var row = new ArchivedBriefRow(
                null,
                event.generatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                sources.country(),
                sources.category(),
                brief.headline(),
                brief.overview(),
                objectMapper.writeValueAsString(brief.topics()),
                sources.articleCount(),
                event.generatedAt());

        repository.save(row);
        log.info("Archived the brief for {}", event.query());
    }
}
