package com.arthur.newsbriefwithia.client;

import com.arthur.newsbriefwithia.dto.Article;
import com.arthur.newsbriefwithia.dto.OllamaRequest;
import com.arthur.newsbriefwithia.dto.OllamaResponse;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class OllamaClient {

    @Value("${ollama.base.url}")
    private String ollamaApiUrl;

    @Value("${ollama.mistral.model}")
    private String ollamaModel;


    public OllamaResponse generateSummary(final List<Article> articles) {
        final RestTemplate restTemplate = new RestTemplate();

        final String prompt = getPrompt(articles);

        final OllamaRequest requestPayload = OllamaRequest.builder()
                .prompt(prompt)
                .model(ollamaModel)
                .stream(false)
                .build();

        final HttpEntity<OllamaRequest> requestEntity = getHttpEntity(requestPayload);

        final ResponseEntity<OllamaResponse> response =
                restTemplate.postForEntity(ollamaApiUrl, requestEntity, OllamaResponse.class);

        log.info("Ollama request response: {}", response.getBody());

        return response.getBody();
    }

    public static HttpEntity<OllamaRequest> getHttpEntity(final OllamaRequest requestPayload) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(requestPayload, headers);
    }

    private String getPrompt(final List<Article> articles) {
        final StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are a news summarizes. " +
                "Summarize the top global news stories from today in a concise and informative way. " +
                "Focus on major events, political developments, economic updates , and major technology or" +
                "science breakthroughs. Keep the summary clear , objective, and easy to read, like a daily news brief. ");

        for(Article article : articles){
            promptBuilder
                    .append("Title: ").append(article.getTitle()).append("\n")
                    .append("Description: ").append(article.getDescription()).append("\n")
                    .append("End of article\n\n");
        }

        final String prompt = promptBuilder.toString();
        return prompt;
    }
}
