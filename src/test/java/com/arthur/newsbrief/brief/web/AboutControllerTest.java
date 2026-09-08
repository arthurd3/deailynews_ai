package com.arthur.newsbrief.brief.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AboutController.class)
class AboutControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void explainsTheArchitectureInsideTheApplication() {
        assertThat(mvc.get().uri("/about").exchange())
                .hasStatusOk()
                .bodyText()
                .contains("How this is built")
                .contains("internal");
    }
}
