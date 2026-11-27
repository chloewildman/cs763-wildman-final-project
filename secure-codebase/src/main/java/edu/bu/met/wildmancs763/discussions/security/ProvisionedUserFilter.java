/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions.security;

import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
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

@Component
public class ProvisionedUserFilter extends OncePerRequestFilter {

    private final UserAccountService userAccountService;

    public ProvisionedUserFilter(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean loginEndpoint = "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
        boolean passwordSetup = "PUT".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/password".equals(request.getRequestURI());
        if (authentication != null && authentication.isAuthenticated()
                && !loginEndpoint && !"anonymousUser".equals(authentication.getName())
                && !CallerIdentity.isAdministrator(authentication)
                && (!userAccountService.isProvisioned(authentication.getName())
                || (!passwordSetup && !userAccountService.isPasswordSet(authentication.getName())))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"You are not allowed to perform this action.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
