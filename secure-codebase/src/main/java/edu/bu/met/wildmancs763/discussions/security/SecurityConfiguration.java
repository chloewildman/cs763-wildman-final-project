/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Configuration
public class SecurityConfiguration {

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("[A-Za-z0-9._:@-]{1,100}");

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : invalidToken("Invalid audience.");
        OAuth2TokenValidator<Jwt> requiredClaimsValidator = jwt -> {
            Object roles = jwt.getClaims().get("roles");
            boolean validRoles = roles instanceof Collection<?> values
                    && !values.isEmpty()
                    && values.stream().allMatch(String.class::isInstance);
            boolean validSubject = jwt.getSubject() != null
                    && SUBJECT_PATTERN.matcher(jwt.getSubject()).matches();
            boolean trustedKeySource = !jwt.getHeaders().containsKey("jku")
                    && !jwt.getHeaders().containsKey("x5u");
            return jwt.getExpiresAt() != null && validRoles && validSubject && trustedKeySource
                    ? OAuth2TokenValidatorResult.success()
                    : invalidToken("Required claims are missing or invalid.");
        };
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceValidator,
                requiredClaimsValidator));
        return decoder;
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromRoles);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
            AuditLoggingFilter auditLoggingFilter,
            RequestRateLimitFilter requestRateLimitFilter,
            ProvisionedUserFilter provisionedUserFilter) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/discussions/posts/*/moderation",
                                "/api/v1/discussions/posts/*/comments/*/moderation")
                        .hasAnyRole("FACULTY_INSTRUCTOR", "ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "Authentication required."))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "Authentication required."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "You are not allowed to perform this action.")))
                .addFilterAfter(auditLoggingFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(requestRateLimitFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(provisionedUserFilter, RequestRateLimitFilter.class);
        return http.build();
    }

    private Collection<GrantedAuthority> authoritiesFromRoles(Jwt jwt) {
        Collection<?> roles = (Collection<?>) jwt.getClaims().get("roles");
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object roleValue : roles) {
            String role = ((String) roleValue).toLowerCase(Locale.ROOT);
            String grantedRole = switch (role) {
                case "admin" -> "ROLE_ADMIN";
                case "faculty_instructor", "faculty" -> "ROLE_FACULTY_INSTRUCTOR";
                case "student_peer" -> "ROLE_STUDENT_PEER";
                case "student" -> "ROLE_STUDENT";
                default -> "ROLE_STUDENT";
            };
            authorities.add(new SimpleGrantedAuthority(grantedRole));
        }
        return authorities;
    }

    private static OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }

    private static void writeError(
            jakarta.servlet.http.HttpServletResponse response,
            int status,
            String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
