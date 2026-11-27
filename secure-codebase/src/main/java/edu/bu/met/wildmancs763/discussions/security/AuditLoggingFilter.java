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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLoggingFilter.class);
    private static final Pattern COMMENT_ID =
            Pattern.compile("/comments/([A-Za-z0-9-]{1,100})(?:/|$)");
    private static final Pattern RESOURCE_ID =
            Pattern.compile("/(?:posts|comments|users)/([A-Za-z0-9-]{1,100})(?:/|$)");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String subject = authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())
                    ? authentication.getName()
                    : "anonymous";
            String outcome = response.getStatus() >= 400
                    ? (response.getStatus() == 401 || response.getStatus() == 403 ? "DENIED" : "FAILURE")
                    : "SUCCESS";
            LOGGER.info("audit username={} action={} resourceId={} outcome={} timestamp={}",
                    subject,
                    request.getMethod() + " " + request.getRequestURI(),
                    resourceId(request, response),
                    outcome,
                    Instant.now());
        }
    }

    private String resourceId(HttpServletRequest request, HttpServletResponse response) {
        String location = response.getHeader("Location");
        if (location != null) {
            String[] segments = location.split("/");
            return segments[segments.length - 1];
        }
        Matcher commentMatcher = COMMENT_ID.matcher(request.getRequestURI());
        if (commentMatcher.find()) {
            return commentMatcher.group(1);
        }
        Matcher matcher = RESOURCE_ID.matcher(request.getRequestURI());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return request.getRequestURI().startsWith("/actuator/") ? "system" : "collection";
    }
}
