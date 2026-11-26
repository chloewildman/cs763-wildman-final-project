package edu.bu.met.wildmancs763.discussions.api;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.IdentityResponse;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.PasswordSetRequest;
import edu.bu.met.wildmancs763.discussions.security.CallerIdentity;
import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/login")
    public IdentityResponse login(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new BadCredentialsException("Authentication failed.");
        }
        Jwt jwt = jwtAuthentication.getToken();
        return new IdentityResponse(jwt.getSubject(), roles(jwt));
    }

    @GetMapping("/me")
    public IdentityResponse currentUser(Authentication authentication) {
        return new IdentityResponse(
                CallerIdentity.subject(authentication),
                CallerIdentity.roles(authentication));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(
            @Valid @RequestBody PasswordSetRequest request,
            Authentication authentication) {
        userAccountService.setPassword(CallerIdentity.subject(authentication), request.password());
    }

    private List<String> roles(Jwt jwt) {
        return ((List<?>) jwt.getClaims().get("roles")).stream()
                .map(String.class::cast)
                .toList();
    }
}
