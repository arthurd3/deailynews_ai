package com.arthur.newsbrief.summarization.internal;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.arthur.newsbrief.summarization.NewsSummary;
import com.arthur.newsbrief.summarization.SourceDocument;
import com.arthur.newsbrief.summarization.SummarizationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Covers the prompt-shaping and failure-translation logic around the model call. */
class SpringAiSummaryGeneratorTest {

    private static final NewsSummary SUMMARY =
            new NewsSummary("Headline", "Overview", List.of());

    @SuppressWarnings("unchecked")
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }

    private SpringAiSummaryGenerator generatorWith(int maxDocuments) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultSystem(any(Resource.class))).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        return new SpringAiSummaryGenerator(builder, new SummarizationProperties(maxDocuments),
                prompt("system"), prompt("brief {articles}"));
    }

    @Test
    void returnsTheStructuredSummaryFromTheModel() {
        when(responseSpec.entity(NewsSummary.class)).thenReturn(SUMMARY);

        assertThat(generatorWith(10).summarize(List.of(new SourceDocument("A", "a"))))
                .isEqualTo(SUMMARY);
    }

    @Test
    void capsThePromptAtTheConfiguredNumberOfDocuments() {
        when(responseSpec.entity(NewsSummary.class)).thenReturn(SUMMARY);

        List<SourceDocument> documents = IntStream.rangeClosed(1, 25)
                .mapToObj(index -> new SourceDocument("Story " + index, "Body " + index))
                .toList();

        generatorWith(3).summarize(documents);

        // An unbounded prompt eventually overruns the model's context window, so the
        // cap has to actually reach the rendered text rather than just the log line.
        assertThat(renderedArticles()).contains("Story 1", "Story 3").doesNotContain("Story 4");
    }

    @Test
    void includesTitleAndDescriptionOfEachDocument() {
        when(responseSpec.entity(NewsSummary.class)).thenReturn(SUMMARY);

        generatorWith(10).summarize(List.of(
                new SourceDocument("Rates held", "The central bank paused."),
                new SourceDocument("No body", null)));

        assertThat(renderedArticles())
                .contains("1. Rates held")
                .contains("The central bank paused.")
                .contains("2. No body");
    }

    @Test
    void rejectsAnEmptyDocumentListWithoutCallingTheModel() {
        assertThatThrownBy(() -> generatorWith(10).summarize(List.of()))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("no articles");

        verify(chatClient, never()).prompt();
    }

    @Test
    void translatesAnUnreachableModelIntoASummarizationFailure() {
        when(responseSpec.entity(NewsSummary.class))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> generatorWith(10).summarize(List.of(new SourceDocument("A", "a"))))
                .isInstanceOf(SummarizationException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void translatesAnUnparseableReplyIntoASummarizationFailure() {
        when(responseSpec.entity(NewsSummary.class)).thenReturn(null);

        assertThatThrownBy(() -> generatorWith(10).summarize(List.of(new SourceDocument("A", "a"))))
                .isInstanceOf(SummarizationException.class)
                .hasMessageContaining("unparseable");
    }

    /** Replays the prompt-building callback to see what text the model would receive. */
    @SuppressWarnings("unchecked")
    private String renderedArticles() {
        ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).user(captor.capture());

        ChatClient.PromptUserSpec userSpec = mock(ChatClient.PromptUserSpec.class);
        when(userSpec.text(any(Resource.class))).thenReturn(userSpec);
        when(userSpec.param(eq("articles"), any())).thenReturn(userSpec);

        captor.getValue().accept(userSpec);

        ArgumentCaptor<Object> articles = ArgumentCaptor.forClass(Object.class);
        verify(userSpec).param(eq("articles"), articles.capture());
        return String.valueOf(articles.getValue());
    }

    private static Resource prompt(String text) {
        return new ByteArrayResource(text.getBytes());
    }
}
