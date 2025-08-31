package com.arthur.newsbriefwithia.controller;

import com.arthur.newsbriefwithia.dto.NewsSummaryResponse;
import com.arthur.newsbriefwithia.service.NewsBriefService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.print.attribute.standard.Media;
import java.awt.*;

@RestController
@RequestMapping("/api/v1/news-brief")
public class NewsBriefController {

    private final NewsBriefService newsBriefService;

    public NewsBriefController(NewsBriefService newsBriefService) {
        this.newsBriefService = newsBriefService;
    }

    @GetMapping(value = "/general-brief" , produces = MediaType.APPLICATION_JSON_VALUE)
    public NewsSummaryResponse generalBrief(){
        return newsBriefService.generateGeneralNewsBrief(false);
    }

    @GetMapping(value = "/general-brief/render" , produces = MediaType.TEXT_HTML_VALUE)
    public NewsSummaryResponse generalBriefUI(){
        return newsBriefService.generateGeneralNewsBrief(true);
    }


}
