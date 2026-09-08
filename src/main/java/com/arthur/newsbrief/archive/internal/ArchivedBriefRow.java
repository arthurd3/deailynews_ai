package com.arthur.newsbrief.archive.internal;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The archive row as it sits in Postgres.
 *
 * <p>Topics are stored as a JSON string rather than a child table. They are only ever read
 * back whole, alongside their brief, so a second table and a join would buy nothing.
 */
@Table("archived_brief")
record ArchivedBriefRow(

        @Id Long id,
        @Column("day") LocalDate day,
        @Column("country") String country,
        @Column("category") String category,
        @Column("headline") String headline,
        @Column("overview") String overview,
        @Column("topics_json") String topicsJson,
        @Column("article_count") int articleCount,
        @Column("generated_at") Instant generatedAt) {
}
