/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private final RequestRateLimiter rateLimiter;

    public RequestRateLimitFilter(RequestRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String key = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                ? authentication.getName()
                : request.getRemoteAddr();
        RequestRateLimiter.Decision decision = rateLimiter.check(key, isExpensive(request));
        if (!decision.allowed()) {
            long seconds = Math.max(1, TimeUnit.MILLISECONDS.toSeconds(
                    decision.retryAfterMillis() + 999));
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(seconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Request rate limit exceeded.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isExpensive(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return request.getParameter("keyword") != null
                || "text".equalsIgnoreCase(request.getParameter("format"))
                || path.endsWith("/timeline");
    }
}
