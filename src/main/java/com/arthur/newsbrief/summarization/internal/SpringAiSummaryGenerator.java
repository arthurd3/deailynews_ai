package com.arthur.newsbrief.summarization.internal;

import java.util.List;

import com.arthur.newsbrief.summarization.NewsSummary;
import com.arthur.newsbrief.summarization.SourceDocument;
import com.arthur.newsbrief.summarization.SummarizationException;
import com.arthur.newsbrief.summarization.SummaryGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Produces summaries with a language model through Spring AI's {@link ChatClient}.
 *
 * <p>The reply is requested as a typed {@link NewsSummary} rather than free text.
 * Spring AI derives a JSON schema from the record, appends it to the prompt and parses
 * the response back — so the model returns data, not prose that later code has to
 * scrape.
 *
 * <p>Prompts live in {@code resources/prompts} so wording can be tuned without a
 * recompile, and reviewed as text rather than as escaped string concatenation.
 */
@Component
class SpringAiSummaryGenerator implements SummaryGenerator {

    static final String CIRCUIT = "ollama";

    private static final Logger log = LoggerFactory.getLogger(SpringAiSummaryGenerator.class);

    private final ChatClient chatClient;
    private final SummarizationProperties properties;
    private final Resource userPrompt;

    SpringAiSummaryGenerator(ChatClient.Builder chatClientBuilder,
                             SummarizationProperties properties,
                             @Value("classpath:prompts/system.st") Resource systemPrompt,
                             @Value("classpath:prompts/daily-brief.st") Resource userPrompt) {
        this.chatClient = chatClientBuilder.defaultSystem(systemPrompt).build();
        this.properties = properties;
        this.userPrompt = userPrompt;
    }

    @Override
    @CircuitBreaker(name = CIRCUIT)
    public NewsSummary summarize(List<SourceDocument> documents) {
        if (documents.isEmpty()) {
            throw new SummarizationException("There are no articles to summarize", null);
        }

        List<SourceDocument> selected = documents.stream()
                .limit(properties.maxDocuments())
                .toList();

        if (selected.size() < documents.size()) {
            log.debug("Summarizing {} of {} available documents", selected.size(), documents.size());
        }

        try {
            NewsSummary summary = chatClient.prompt()
                    .user(prompt -> prompt.text(userPrompt).param("articles", render(selected)))
                    .call()
                    .entity(NewsSummary.class);

            if (summary == null) {
                throw new SummarizationException("The model returned an unparseable summary", null);
            }
            return summary;
        }
        catch (SummarizationException ex) {
            throw ex;
        }
        catch (RuntimeException ex) {
            // Covers an unreachable Ollama, a missing model and a reply that does not
            // match the requested schema — all of which are upstream problems, not bugs.
            throw new SummarizationException("The summarization model could not produce a brief", ex);
        }
    }

    /** Flattens the documents into the numbered block the prompt template expects. */
    private static String render(List<SourceDocument> documents) {
        StringBuilder rendered = new StringBuilder();

        for (int index = 0; index < documents.size(); index++) {
            SourceDocument document = documents.get(index);
            rendered.append(index + 1).append(". ").append(document.title()).append('\n');

            if (document.description() != null && !document.description().isBlank()) {
                rendered.append("   ").append(document.description()).append('\n');
            }
        }
        return rendered.toString();
    }
}
