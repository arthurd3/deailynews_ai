package com.arthur.newsbrief.shared.error;

import java.net.URI;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates uncaught exceptions into RFC 9457 problem details.
 *
 * <p>Spring MVC's own exceptions (validation failures, unsupported media types, …) are
 * already rendered as problem details because {@code spring.mvc.problemdetails.enabled}
 * is on, so only the application's own failure modes need handling here.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final URI UPSTREAM_PROBLEM = URI.create("urn:newsbrief:problem:upstream-unavailable");
    private static final URI INTERNAL_PROBLEM = URI.create("urn:newsbrief:problem:internal-error");

    @ExceptionHandler(UpstreamUnavailableException.class)
    ProblemDetail handleUpstreamUnavailable(UpstreamUnavailableException ex) {
        log.warn("Upstream '{}' is unavailable: {}", ex.upstream(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Upstream service unavailable");
        problem.setType(UPSTREAM_PROBLEM);
        problem.setProperty("upstream", ex.upstream());
        return problem;
    }

    /**
     * An open circuit breaker means the dependency is already known to be failing, so the
     * request was rejected without being attempted. That is the same outcome for the
     * caller as the upstream being down, and deserves the same status.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail handleOpenCircuit(CallNotPermittedException ex) {
        String upstream = ex.getCausingCircuitBreakerName();
        log.warn("Circuit '{}' is open; rejecting the call without attempting it", upstream);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The %s dependency is unavailable and calls to it are paused.".formatted(upstream));
        problem.setTitle("Upstream service unavailable");
        problem.setType(UPSTREAM_PROBLEM);
        problem.setProperty("upstream", upstream);
        return problem;
    }

    /**
     * Last line of defence. The real cause goes to the log, never to the caller — an
     * unexpected stack trace can carry connection strings or credentials.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception while serving request", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed.");
        problem.setTitle("Internal server error");
        problem.setType(INTERNAL_PROBLEM);
        return problem;
    }
}
