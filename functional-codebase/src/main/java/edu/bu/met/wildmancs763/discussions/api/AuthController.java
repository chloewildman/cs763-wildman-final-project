package edu.bu.met.wildmancs763.discussions.api;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CsrfResponse;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserLoginRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserRegistrationRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserResponse;
import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final UserAccountService userAccountService;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            UserAccountService userAccountService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {
        return userAccountService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid username or password.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username().trim(), request.password()));
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return userAccountService.getUser(authentication.getName());
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return userAccountService.getUser(authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        new SecurityContextLogoutHandler().logout(servletRequest, servletResponse, authentication);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
    }
}
