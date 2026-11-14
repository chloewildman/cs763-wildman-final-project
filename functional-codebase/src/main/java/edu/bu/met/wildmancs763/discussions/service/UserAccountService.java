package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserProvisionRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserRegistrationRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserResponse;
import edu.bu.met.wildmancs763.discussions.api.ResourceConflictException;
import edu.bu.met.wildmancs763.discussions.api.ResourceNotFoundException;
import edu.bu.met.wildmancs763.discussions.domain.DiscussionUser;
import edu.bu.met.wildmancs763.discussions.domain.UserRole;
import edu.bu.met.wildmancs763.discussions.repository.DiscussionUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Transactional
public class UserAccountService {

    private final DiscussionUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            DiscussionUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRegistrationRequest request) {
        return createUser(request.username(), request.password(), UserRole.STUDENT);
    }

    public UserResponse provision(UserProvisionRequest request) {
        return createUser(request.username(), request.password(), request.role());
    }

    public UserResponse getUser(String username) {
        return userRepository.findByUsername(normalizeUsername(username))
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    public void createBootstrapAdmin(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePasswordBytes(password);
        DiscussionUser existingUser = userRepository.findByUsername(normalizedUsername).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "The configured bootstrap administrator username is already assigned to a non-admin user.");
            }
            return;
        }
        persistUser(normalizedUsername, password, UserRole.ADMIN);
    }

    private UserResponse createUser(String username, String password, UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        validatePasswordBytes(password);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResourceConflictException("Username is already registered.");
        }
        return toResponse(persistUser(normalizedUsername, password, role));
    }

    private DiscussionUser persistUser(String username, String password, UserRole role) {
        try {
            return userRepository.saveAndFlush(new DiscussionUser(
                    username, passwordEncoder.encode(password), role));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Username is already registered.");
        }
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username must contain 3 to 30 letters, numbers, dots, underscores, or hyphens.");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9._-]{3,30}$")) {
            throw new IllegalArgumentException("Username must contain 3 to 30 letters, numbers, dots, underscores, or hyphens.");
        }
        return normalized;
    }

    private void validatePasswordBytes(String password) {
        if (password.length() < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be no more than 72 UTF-8 bytes.");
        }
    }

    private UserResponse toResponse(DiscussionUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
