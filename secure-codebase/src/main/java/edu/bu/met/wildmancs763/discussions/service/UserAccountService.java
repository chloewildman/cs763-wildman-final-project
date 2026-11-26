package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserProvisionRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserResponse;
import edu.bu.met.wildmancs763.discussions.api.ResourceConflictException;
import edu.bu.met.wildmancs763.discussions.api.ResourceNotFoundException;
import edu.bu.met.wildmancs763.discussions.domain.DiscussionUser;
import edu.bu.met.wildmancs763.discussions.repository.DiscussionUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserAccountService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^*()_+\\-=\\[\\]{}.,?:;])[^<>/&\"']{16,72}$");

    private final DiscussionUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            DiscussionUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse provision(UserProvisionRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ResourceConflictException("User could not be provisioned.");
        }
        try {
            DiscussionUser user = userRepository.saveAndFlush(
                    new DiscussionUser(username, "UNSET:", request.role()));
            return toResponse(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("User could not be provisioned.");
        }
    }

    public void setPassword(String subject, String password) {
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException(
                    "Password must be 16 to 72 characters and include uppercase, lowercase, a digit, and an allowed special character.");
        }
        String username = normalizeUsername(subject);
        DiscussionUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public boolean isProvisioned(String subject) {
        if (!StringUtils.hasText(subject)) {
            return false;
        }
        String normalized = subject.trim().toLowerCase(Locale.ROOT);
        return USERNAME_PATTERN.matcher(normalized).matches()
                && userRepository.existsByUsername(normalized);
    }

    @Transactional(readOnly = true)
    public boolean isPasswordSet(String subject) {
        if (!StringUtils.hasText(subject)) {
            return false;
        }
        String normalized = subject.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        return userRepository.findByUsername(normalized)
                .map(DiscussionUser::isPasswordSet)
                .orElse(false);
    }

    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username must contain 3 to 30 letters, numbers, dots, underscores, or hyphens.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Username must contain 3 to 30 letters, numbers, dots, underscores, or hyphens.");
        }
        return normalized;
    }

    private UserResponse toResponse(DiscussionUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
