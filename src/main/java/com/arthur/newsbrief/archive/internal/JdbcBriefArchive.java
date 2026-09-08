package com.arthur.newsbrief.archive.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.arthur.newsbrief.archive.ArchivedBrief;
import com.arthur.newsbrief.archive.BriefArchive;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Reads the archive back and maps rows onto the module's public record. */
@Component
class JdbcBriefArchive implements BriefArchive {

    private static final TypeReference<List<ArchivedBrief.Topic>> TOPICS = new TypeReference<>() { };

    private final ArchivedBriefRepository repository;
    private final ObjectMapper objectMapper;

    JdbcBriefArchive(ArchivedBriefRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ArchivedBrief> recent(int limit) {
        return repository.findRecent(limit).stream().map(this::toArchivedBrief).toList();
    }

    @Override
    public Optional<ArchivedBrief> forDay(LocalDate day, String country, String category) {
        return repository.findForDay(day, country, category).map(this::toArchivedBrief);
    }

    @Override
    public long count() {
        return repository.count();
    }

    private ArchivedBrief toArchivedBrief(ArchivedBriefRow row) {
        return new ArchivedBrief(
                row.id(), row.day(), row.country(), row.category(),
                row.headline(), row.overview(),
                objectMapper.readValue(row.topicsJson(), TOPICS),
                row.generatedAt());
    }
}
