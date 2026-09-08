package com.arthur.newsbrief.summarization.internal;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Chooses which model answers, from configuration.
 *
 * <p>With two Spring AI starters on the classpath there are two {@code ChatModel} beans, so the
 * auto-configured {@code ChatClient.Builder} is ambiguous. Building the client explicitly from
 * the selected model settles that and makes the choice visible in one place.
 *
 * <p>Ollama is the default: it is the whole point of the project that summarization can run
 * without sending anything to a hosted provider. Anthropic exists as the documented fallback
 * for when a small local model will not honour the structured-output schema.
 */
@Configuration(proxyBeanMethods = false)
class SummarizationChatClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "newsbrief.summarization.provider",
            havingValue = "ollama", matchIfMissing = true)
    ChatClient ollamaSummarizationChatClient(
            OllamaChatModel chatModel,
            @Value("classpath:prompts/system.st") Resource systemPrompt) {

        return ChatClient.builder(chatModel).defaultSystem(systemPrompt).build();
    }

    @Bean
    @ConditionalOnProperty(name = "newsbrief.summarization.provider", havingValue = "anthropic")
    ChatClient anthropicSummarizationChatClient(
            AnthropicChatModel chatModel,
            @Value("classpath:prompts/system.st") Resource systemPrompt) {

        return ChatClient.builder(chatModel).defaultSystem(systemPrompt).build();
    }
}
