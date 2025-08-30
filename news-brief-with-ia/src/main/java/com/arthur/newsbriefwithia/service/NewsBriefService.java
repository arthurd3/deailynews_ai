package com.arthur.newsbriefwithia.service;

import com.arthur.newsbriefwithia.dto.NewsSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NewsBriefService {

    public NewsSummaryResponse generateGeneralNewsBrief() {
        final NewsApiResponse newsApiResponse = newsApiClient.getTopHeadlines();
    }
}
