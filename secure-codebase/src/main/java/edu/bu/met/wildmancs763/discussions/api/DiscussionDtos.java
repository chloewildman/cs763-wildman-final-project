/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

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

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^*()_+\\-=\\[\\]{}.,?:;])[^<>/&\"']{16,72}$";

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
            OffsetDateTime updatedAt,
            boolean adminModified,
            String adminModifiedBy,
            String moderationNote,
            String moderatedBySub) {
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
            OffsetDateTime updatedAt,
            boolean adminModified,
            String adminModifiedBy,
            String moderationNote,
            String moderatedBySub) {
    }

    public record ModerationRequest(
            @NotNull ModerationStatus status,
            @Size(max = 1000) String note) {
    }

    public record UserProvisionRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$") String username,
            @NotNull UserRole role) {
    }

    public record PasswordSetRequest(
            @NotBlank
            @Pattern(regexp = PASSWORD_PATTERN,
                    message = "must be 16 to 72 characters and include uppercase, lowercase, a digit, and an allowed special character")
            String password) {
    }

    public record UserResponse(UUID id, String username, UserRole role, OffsetDateTime createdAt) {
    }

    public record IdentityResponse(String subject, List<String> roles) {
    }

    public record ApiError(String error) {
    }
}
