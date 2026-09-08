package com.arthur.newsbrief.summarization.internal;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The payoff of having a port: which model answers is configuration, not code.
 *
 * <p>Both chat models are stubbed, so this proves the wiring without needing Ollama running or
 * an Anthropic key.
 */
class SummarizationProviderSelectionTest {

    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(StubChatModels.class, SummarizationChatClientConfiguration.class);

    @Test
    void defaultsToTheLocalModel() {
        contexts.run(context -> assertThat(context)
                .as("nothing should leave the machine unless it was asked for")
                .hasBean("ollamaSummarizationChatClient")
                .doesNotHaveBean("anthropicSummarizationChatClient"));
    }

    @Test
    void selectsTheLocalModelWhenAskedExplicitly() {
        contexts.withPropertyValues("newsbrief.summarization.provider=ollama")
                .run(context -> assertThat(context).hasBean("ollamaSummarizationChatClient"));
    }

    @Test
    void switchesToTheHostedModelByConfigurationAlone() {
        contexts.withPropertyValues("newsbrief.summarization.provider=anthropic")
                .run(context -> assertThat(context)
                        .hasBean("anthropicSummarizationChatClient")
                        .doesNotHaveBean("ollamaSummarizationChatClient"));
    }

    @Test
    void exactlyOneChatClientIsEverDefined() {
        contexts.withPropertyValues("newsbrief.summarization.provider=anthropic")
                .run(context -> assertThat(context.getBeansOfType(ChatClient.class))
                        .as("an ambiguous ChatClient would fail the summarizer at startup")
                        .hasSize(1));
    }

    @Configuration(proxyBeanMethods = false)
    static class StubChatModels {

        @Bean
        OllamaChatModel ollamaChatModel() {
            return mock(OllamaChatModel.class);
        }

        @Bean
        AnthropicChatModel anthropicChatModel() {
            return mock(AnthropicChatModel.class);
        }
    }
}
