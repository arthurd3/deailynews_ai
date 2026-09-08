package com.arthur.newsbrief.brief.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explains how the application is put together, inside the application itself. */
@Controller
class AboutController {

    @GetMapping("/about")
    String about() {
        return "about";
    }
}
