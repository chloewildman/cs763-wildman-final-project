package edu.bu.met.wildmancs763.discussions.api;

import edu.bu.met.wildmancs763.discussions.domain.ModerationStatus;
import edu.bu.met.wildmancs763.discussions.domain.PostCategory;
import edu.bu.met.wildmancs763.discussions.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DiscussionDtos {

    private DiscussionDtos() {
    }

    public record PostRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 100000) String body,
            @NotNull PostCategory category,
            @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
            @Size(max = 100) String projectId,
            @Size(max = 100) String courseId,
            @Size(max = 100) String labId) {
    }

    public record PostResponse(
            UUID id,
            String title,
            String body,
            PostCategory category,
            List<String> tags,
            String scopeType,
            String scopeId,
            String projectId,
            String courseId,
            String labId,
            String authorHandle,
            ModerationStatus moderationStatus,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record CommentRequest(
            @NotBlank @Size(max = 10000) String message,
            UUID parentCommentId) {
    }

    public record CommentUpdateRequest(
            @NotBlank @Size(max = 10000) String message) {
    }

    public record CommentResponse(
            UUID id,
            UUID postId,
            UUID parentCommentId,
            String message,
            String authorHandle,
            ModerationStatus moderationStatus,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record ModerationRequest(@NotNull ModerationStatus status) {
    }

    public record UserRegistrationRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$") String username,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record UserLoginRequest(
            @NotBlank @Size(max = 30) String username,
            @NotBlank @Size(max = 72) String password) {
    }

    public record UserProvisionRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$") String username,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotNull UserRole role) {
    }

    public record UserResponse(UUID id, String username, UserRole role, OffsetDateTime createdAt) {
    }

    public record CsrfResponse(String token) {
    }

    public record ApiError(String error) {
    }
}
