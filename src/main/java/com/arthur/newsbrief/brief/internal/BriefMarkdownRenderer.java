package com.arthur.newsbrief.brief.internal;

import com.arthur.newsbrief.brief.DailyBrief;
import org.springframework.stereotype.Component;

/**
 * Renders a brief as markdown, so it can be pasted into a note, a ticket or a chat.
 *
 * <p>This is a third representation of the same resource rather than a client-side
 * transformation: the copy button in the UI fetches it instead of reassembling the text in
 * JavaScript, which keeps one definition of what a brief looks like as prose.
 */
@Component
public class BriefMarkdownRenderer {

    public static final String TEXT_MARKDOWN = "text/markdown";

    public String render(DailyBrief brief) {
        StringBuilder out = new StringBuilder();

        out.append("# ").append(brief.headline()).append("\n\n");
        out.append(brief.overview()).append("\n");

        for (DailyBrief.Topic topic : brief.topics()) {
            out.append("\n## ").append(topic.title()).append('\n');

            if (topic.category() != null) {
                out.append('`').append(topic.category()).append("`\n");
            }
            out.append('\n').append(topic.summary()).append('\n');
        }

        DailyBrief.Sources sources = brief.sources();
        out.append("\n---\n\n")
                .append("_Summarized from ").append(sources.articleCount()).append(" headlines (")
                .append(sources.country());

        if (sources.category() != null) {
            out.append(" / ").append(sources.category());
        }
        out.append(") on ").append(brief.generatedAt()).append(".")
                .append(" Source data from NewsAPI; summary written by a local language model._\n");

        return out.toString();
    }
}
