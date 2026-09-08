package com.arthur.newsbrief.brief;

import java.time.Instant;
import java.util.List;

/**
 * The public representation of a generated brief.
 *
 * <p>Kept separate from the summarization module's {@code NewsSummary} so the published
 * API contract can stay stable while the model's output schema evolves, and so callers
 * can see where the material came from.
 *
 * @param headline    banner line for the brief
 * @param overview    the day in two or three sentences
 * @param topics      individual stories
 * @param sources     provenance of the underlying headlines
 * @param generatedAt when this brief was assembled
 */
public record DailyBrief(String headline, String overview, List<Topic> topics, Sources sources,
                         Instant generatedAt) {

    public DailyBrief {
        topics = List.copyOf(topics);
    }

    /**
     * @param title    the story in a few words
     * @param summary  what happened and why it matters
     * @param category broad subject area
     */
    public record Topic(String title, String summary, String category) {
    }

    /**
     * @param country      country the headlines were drawn from
     * @param category     category filter applied, or {@code null} for all
     * @param articleCount how many articles fed the summary
     * @param retrievedAt  when the headlines were fetched
     */
    public record Sources(String country, String category, int articleCount, Instant retrievedAt) {
    }
}
