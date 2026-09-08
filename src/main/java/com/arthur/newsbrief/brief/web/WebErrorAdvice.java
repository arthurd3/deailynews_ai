package com.arthur.newsbrief.brief.web;

import com.arthur.newsbrief.shared.error.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Renders failures as pages for the browser-facing routes.
 *
 * <p>Scoped to this package on purpose. {@code ApiExceptionHandler} is a global
 * {@code @RestControllerAdvice}, so without this the browser would receive an RFC 9457
 * problem document as raw JSON whenever Ollama or NewsAPI went down — correct for a client
 * library, useless for a person. This advice takes precedence for the pages; the API keeps
 * its problem details untouched.
 */
@ControllerAdvice(basePackages = "com.arthur.newsbrief.brief.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
class WebErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(WebErrorAdvice.class);

    @ExceptionHandler(UpstreamUnavailableException.class)
    ModelAndView upstreamUnavailable(UpstreamUnavailableException ex) {
        log.warn("Upstream '{}' unavailable while rendering a page: {}", ex.upstream(), ex.getMessage());
        return errorPage(ex.upstream(), ex.getMessage());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    ModelAndView circuitOpen(CallNotPermittedException ex) {
        String upstream = ex.getCausingCircuitBreakerName();
        log.warn("Circuit '{}' is open; not attempting the call", upstream);
        return errorPage(upstream,
                "Calls to this dependency are paused after repeated failures.");
    }

    /**
     * A hand-typed country or category should not hand the browser a JSON problem document.
     * Handled explicitly because this advice runs ahead of Boot's problem-detail handler.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ModelAndView invalidParameters(HandlerMethodValidationException ex) {
        ModelAndView page = new ModelAndView("error/bad-request", HttpStatus.BAD_REQUEST);
        page.addObject("detail",
                "Country must be a two-letter code, and category must be one the provider knows.");
        return page;
    }

    /**
     * Fallback for the pages. The cause goes to the log, never to the reader - an unexpected
     * stack trace can carry connection strings. Spring resolves the most specific handler
     * first, so the cases above still win.
     */
    @ExceptionHandler(Exception.class)
    ModelAndView unexpected(Exception ex) {
        log.error("Unhandled exception while rendering a page", ex);
        return new ModelAndView("error/unexpected", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ModelAndView errorPage(String upstream, String detail) {
        ModelAndView page = new ModelAndView("error/unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        page.addObject("upstream", upstream);
        page.addObject("detail", detail);
        return page;
    }
}
