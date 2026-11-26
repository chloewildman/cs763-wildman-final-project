// Tool: Claude (Anthropic), via Claude Code
// Prompt Summary: "Write a Spring Boot resource server config that checks JWTs from
//   the mock token issuer's JWKS endpoint."
// AI Contribution: Initial draft (~90%)
// Human Modifications:
// - Adapt package names and paths to your project
// Verification:
// - Not compiled or run yet (see the README and the improvement plan)
// Confidence: Low
//
// Example only. Adapt package names and paths to your project.
// Requires: spring-boot-starter-oauth2-resource-server
//
// application.properties
//   app.jwt.jwk-set-uri=http://localhost:9000/.well-known/jwks.json   (in Docker Compose: http://token-issuer:9000/...)
//   app.jwt.issuer=http://localhost:9000
//   app.jwt.audience=cs763-api

package com.example.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfigExample {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt.jwk-set-uri}") String jwkSetUri,
                        @Value("${app.jwt.issuer}") String issuer,
                        @Value("${app.jwt.audience}") String audience) {
    // Algorithm allowlist: only RS256 and ES256. "none" and HS256 are rejected.
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
        .jwsAlgorithm(SignatureAlgorithm.RS256)
        .jwsAlgorithm(SignatureAlgorithm.ES256)
        .build();

    OAuth2TokenValidator<Jwt> audienceCheck = jwt ->
        jwt.getAudience() != null && jwt.getAudience().contains(audience)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));

    OAuth2TokenValidator<Jwt> expRequired = jwt ->
        jwt.getExpiresAt() != null
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "exp is required", null));

    // createDefaultWithIssuer checks iss, exp and nbf (with a small clock skew)
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer), audienceCheck, expRequired));
    return decoder;
  }
}

/*
 Row-level authorization example in a controller:

  @GetMapping("/api/records/{id}")
  RecordDto get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    Record r = repo.findById(id).orElseThrow(NotFound::new);
    if (!r.getOwnerId().equals(jwt.getSubject())) throw new NotFound();  // 404 hides existence
    return toDto(r);
  }
*/
