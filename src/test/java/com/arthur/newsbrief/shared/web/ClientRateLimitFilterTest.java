package com.arthur.newsbrief.shared.web;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The per-client budget. {@code @ConcurrencyLimit} on the service stops parallel inference;
 * this is what stops one caller walking through categories in sequence.
 */
class ClientRateLimitFilterTest {

    private static final RateLimitProperties LIMIT_OF_TWO =
            new RateLimitProperties(true, 2, Duration.ofMinutes(1), new String[] { "/api/v1/briefs", "/briefs" });

    private final ClientRateLimitFilter filter = new ClientRateLimitFilter(LIMIT_OF_TWO);

    private MockHttpServletResponse call(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void allowsRequestsWithinTheBudget() throws Exception {
        assertThat(call("/briefs/daily", "10.0.0.1").getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(call("/briefs/daily", "10.0.0.1").getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void rejectsTheRequestThatExceedsTheBudget() throws Exception {
        call("/briefs/daily", "10.0.0.2");
        call("/briefs/daily", "10.0.0.2");

        MockHttpServletResponse rejected = call("/briefs/daily", "10.0.0.2");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isNotNull();
        assertThat(rejected.getContentAsString()).contains("rate-limited");
    }

    @Test
    void budgetsAreHeldPerClientRatherThanGlobally() throws Exception {
        call("/briefs/daily", "10.0.0.3");
        call("/briefs/daily", "10.0.0.3");

        assertThat(call("/briefs/daily", "10.0.0.3").getStatus()).isEqualTo(429);
        assertThat(call("/briefs/daily", "10.0.0.4").getStatus())
                .as("one noisy caller must not spend everybody else's budget")
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void honoursTheForwardedClientAddressBehindAProxy() throws Exception {
        MockHttpServletRequest first = new MockHttpServletRequest("GET", "/briefs/daily");
        first.setRemoteAddr("172.16.0.1");                     // the proxy
        first.addHeader("X-Forwarded-For", "203.0.113.9, 172.16.0.1");
        filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest("GET", "/briefs/daily");
        second.setRemoteAddr("172.16.0.1");
        second.addHeader("X-Forwarded-For", "203.0.113.9, 172.16.0.1");
        filter.doFilter(second, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest third = new MockHttpServletRequest("GET", "/briefs/daily");
        third.setRemoteAddr("172.16.0.1");
        third.addHeader("X-Forwarded-For", "203.0.113.9, 172.16.0.1");
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(third, rejected, new MockFilterChain());

        // Without honouring the header every request behind a proxy shares one bucket.
        assertThat(rejected.getStatus()).isEqualTo(429);
    }

    @Test
    void leavesUnrelatedPathsAlone() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(call("/actuator/health", "10.0.0.5").getStatus())
                    .as("health checks must not be rate limited")
                    .isEqualTo(HttpServletResponse.SC_OK);
        }
    }
}
