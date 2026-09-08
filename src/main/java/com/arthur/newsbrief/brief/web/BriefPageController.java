package com.arthur.newsbrief.brief.web;

import com.arthur.newsbrief.brief.internal.BriefProperties;
import com.arthur.newsbrief.brief.internal.NewsBriefService;
import com.arthur.newsbrief.news.HeadlinesQuery;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The pages a person actually looks at.
 *
 * <p>Every route works without JavaScript: the server renders the whole page and a plain
 * link or form submit gets you there. htmx only upgrades that — when it is driving the
 * request it sends {@code HX-Request}, and we return the brief fragment alone so the page
 * can swap it in and show a skeleton meanwhile. Generation takes tens of seconds of local
 * inference, and a browser tab that simply freezes for that long reads as broken.
 *
 * <p>The fragment carries the controls as well as the brief. Swapping only the brief left
 * the category chips highlighting whatever was selected previously.
 */
@Controller
class BriefPageController {

    private static final String COUNTRY_PATTERN = "[a-zA-Z]{2}";
    private static final String CATEGORY_PATTERN =
            "business|entertainment|general|health|science|sports|technology";

    private static final String FULL_PAGE = "brief";
    private static final String FRAGMENT = "fragments/brief :: main";

    private final NewsBriefService newsBriefService;
    private final BriefProperties properties;

    BriefPageController(NewsBriefService newsBriefService, BriefProperties properties) {
        this.newsBriefService = newsBriefService;
        this.properties = properties;
    }

    @GetMapping("/")
    String home(@RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,
                @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category,
                Model model) {

        addControls(model, country, category);
        return "index";
    }

    @GetMapping("/briefs/daily")
    String daily(@RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,
                 @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category,
                 @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
                 Model model) {

        HeadlinesQuery query = queryFor(country, category);
        addControls(model, query.country(), query.category());
        model.addAttribute("brief", newsBriefService.dailyBrief(query));

        return htmxRequest != null ? FRAGMENT : FULL_PAGE;
    }

    /**
     * Discards the cached brief and summarizes again.
     *
     * <p>A POST because it changes stored state, and because a browser must not replay it
     * on a back-navigation — each call is tens of seconds of inference.
     */
    @PostMapping("/briefs/daily")
    String regenerate(@RequestParam(required = false) @Pattern(regexp = COUNTRY_PATTERN) String country,
                      @RequestParam(required = false) @Pattern(regexp = CATEGORY_PATTERN) String category,
                      @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
                      Model model) {

        HeadlinesQuery query = queryFor(country, category);
        addControls(model, query.country(), query.category());
        model.addAttribute("brief", newsBriefService.regenerate(query));

        return htmxRequest != null ? FRAGMENT : FULL_PAGE;
    }

    private HeadlinesQuery queryFor(String country, String category) {
        return new HeadlinesQuery(
                country == null || country.isBlank() ? properties.defaultCountry() : country,
                category);
    }

    /** Everything the controls need to render themselves in the right state. */
    private void addControls(Model model, String country, String category) {
        model.addAttribute("country",
                country == null || country.isBlank() ? properties.defaultCountry() : country);
        model.addAttribute("category", category);
        model.addAttribute("categories", HeadlinesQuery.CATEGORIES.stream().sorted().toList());
    }
}
