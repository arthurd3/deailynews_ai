package com.arthur.newsbrief.archive;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A brief as it was stored, with the provenance needed to show it again later.
 *
 * @param id          storage identity
 * @param day         the day the brief covers, used to browse the archive
 * @param country     country the headlines came from
 * @param category    category filter applied, or {@code null}
 * @param headline    banner line
 * @param overview    the day in a few sentences
 * @param topics      the individual stories
 * @param generatedAt when the brief was assembled
 */
public record ArchivedBrief(Long id, LocalDate day, String country, String category,
                            String headline, String overview, List<Topic> topics,
                            Instant generatedAt) {

    public ArchivedBrief {
        topics = topics == null ? List.of() : List.copyOf(topics);
    }

    /** One story within an archived brief. */
    public record Topic(String title, String summary, String category) {
    }
}
