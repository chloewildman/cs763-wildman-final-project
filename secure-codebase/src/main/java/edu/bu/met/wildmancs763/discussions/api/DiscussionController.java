/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.api;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentResponse;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentUpdateRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.ModerationRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.PostRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.PostResponse;
import edu.bu.met.wildmancs763.discussions.domain.ModerationStatus;
import edu.bu.met.wildmancs763.discussions.domain.PostCategory;
import edu.bu.met.wildmancs763.discussions.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        PostResponse response = discussionService.createPost(request, authentication);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{postId}")
                        .buildAndExpand(response.id())
                        .toUri())
                .body(response);
    }

    @GetMapping("/posts")
    public List<PostResponse> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String labId,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) String authorHandle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            Authentication authentication) {
        return discussionService.listPosts(
                keyword, projectId, courseId, labId, category, authorHandle, from, to, authentication);
    }

    @GetMapping("/posts/{postId}")
    public PostResponse getPost(@PathVariable UUID postId, Authentication authentication) {
        return discussionService.getPost(postId, authentication);
    }

    @PutMapping("/posts/{postId}")
    public PostResponse updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody PostRequest request,
            Authentication authentication) {
        return discussionService.updatePost(
                postId, request, authentication);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            Authentication authentication) {
        discussionService.deletePost(postId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/scopes/{scopeType}/{scopeId}/timeline")
    public List<PostResponse> scopeTimeline(
            @PathVariable String scopeType,
            @PathVariable String scopeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            Authentication authentication) {
        return discussionService.scopeTimeline(
                scopeType, scopeId, keyword, category, from, to, authentication);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        CommentResponse response = discussionService.createComment(
                postId, request, authentication);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{commentId}")
                        .buildAndExpand(response.id())
                        .toUri())
                .body(response);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> listComments(
            @PathVariable UUID postId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String authorHandle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "json") String format,
            Authentication authentication) {
        List<CommentResponse> comments = discussionService.listComments(
                postId, keyword, authorHandle, from, to, authentication);
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(comments);
        }
        if ("text".equalsIgnoreCase(format)) {
            String manifest = comments.stream()
                    .map(comment -> comment.createdAt() + " | " + comment.authorHandle()
                            + " | " + comment.message())
                    .reduce((first, next) -> first + System.lineSeparator() + next)
                    .orElse("");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"comments-" + postId + ".txt\"")
                    .body(manifest);
        }
        throw new InvalidRequestException("format must be json or text.");
    }

    @GetMapping("/posts/{postId}/comments/{commentId}")
    public CommentResponse getComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            Authentication authentication) {
        return discussionService.getComment(postId, commentId, authentication);
    }

    @PutMapping("/posts/{postId}/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication) {
        return discussionService.updateComment(
                postId, commentId, request, authentication);
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            Authentication authentication) {
        discussionService.deleteComment(postId, commentId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/posts/{postId}/moderation")
    public PostResponse moderatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody ModerationRequest request,
            Authentication authentication) {
        return discussionService.moderatePost(postId, request, authentication);
    }

    @PatchMapping("/posts/{postId}/comments/{commentId}/moderation")
    public CommentResponse moderateComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody ModerationRequest request,
            Authentication authentication) {
        return discussionService.moderateComment(postId, commentId, request, authentication);
    }
}
