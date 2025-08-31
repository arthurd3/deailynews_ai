package com.arthur.newsbriefwithia.service;

import com.arthur.newsbriefwithia.client.NewsApiClient;
import com.arthur.newsbriefwithia.client.OllamaClient;
import com.arthur.newsbriefwithia.dto.NewsApiResponse;
import com.arthur.newsbriefwithia.dto.NewsSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class NewsBriefService {

    private final NewsApiClient newsApiClient;
    private final OllamaClient ollamaClient;

    public NewsBriefService(NewsApiClient newsApiClient, OllamaClient ollamaClient) {
        this.newsApiClient = newsApiClient;
        this.ollamaClient = ollamaClient;
    }

    public NewsSummaryResponse generateGeneralNewsBrief() {
        final NewsApiResponse newsApiResponse = newsApiClient.getTopHeadlines();
        log.info("Top Headlines: {}", newsApiResponse);

        final OllamaResponse ollamaResponse =
                ollamaCliente.generateSummary(newsApiResponse.getArticles(), false);

        return NewsSummaryResponse.builder()
                .createdAt(LocalDate.now())
                .summary("This placehoard")
                .build();
    }
}
