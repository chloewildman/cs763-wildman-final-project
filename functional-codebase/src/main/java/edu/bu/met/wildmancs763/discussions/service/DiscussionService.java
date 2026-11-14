package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentResponse;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentUpdateRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.PostRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.PostResponse;
import edu.bu.met.wildmancs763.discussions.api.InvalidRequestException;
import edu.bu.met.wildmancs763.discussions.api.ResourceNotFoundException;
import edu.bu.met.wildmancs763.discussions.domain.DiscussionComment;
import edu.bu.met.wildmancs763.discussions.domain.DiscussionPost;
import edu.bu.met.wildmancs763.discussions.domain.ModerationStatus;
import edu.bu.met.wildmancs763.discussions.domain.PostCategory;
import edu.bu.met.wildmancs763.discussions.domain.ScopeType;
import edu.bu.met.wildmancs763.discussions.repository.DiscussionCommentRepository;
import edu.bu.met.wildmancs763.discussions.repository.DiscussionPostRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@Transactional
public class DiscussionService {

    private final DiscussionPostRepository postRepository;
    private final DiscussionCommentRepository commentRepository;

    public DiscussionService(
            DiscussionPostRepository postRepository,
            DiscussionCommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    public PostResponse createPost(PostRequest request, String authorHandle) {
        DiscussionPost post = new DiscussionPost();
        applyPostRequest(post, request);
        post.setAuthorHandle(authorHandle);
        return toPostResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> listPosts(
            String keyword,
            String projectId,
            String courseId,
            String labId,
            PostCategory category,
            String authorHandle,
            OffsetDateTime from,
            OffsetDateTime to) {
        validateRange(from, to);
        return postRepository.findAll().stream()
                .filter(post -> post.getModerationStatus() != ModerationStatus.HIDDEN_BY_MODERATOR)
                .filter(matchesKeyword(keyword))
                .filter(post -> matchesScope(post, ScopeType.PROJECT, projectId))
                .filter(post -> matchesScope(post, ScopeType.COURSE, courseId))
                .filter(post -> matchesScope(post, ScopeType.LAB, labId))
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> matchesAuthor(post.getAuthorHandle(), authorHandle))
                .filter(post -> withinRange(post.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(DiscussionPost::getCreatedAt).reversed())
                .map(this::toPostResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        return toPostResponse(requireVisiblePost(postId));
    }

    public PostResponse updatePost(
            UUID postId, PostRequest request, String authorHandle, boolean administrator) {
        DiscussionPost post = requirePost(postId);
        ensureCanManage(post.getAuthorHandle(), authorHandle, administrator);
        applyPostRequest(post, request);
        return toPostResponse(postRepository.saveAndFlush(post));
    }

    public void deletePost(UUID postId, String authorHandle, boolean administrator) {
        DiscussionPost post = requirePost(postId);
        ensureCanManage(post.getAuthorHandle(), authorHandle, administrator);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> scopeTimeline(
            String scopeTypeValue,
            String scopeId,
            String keyword,
            PostCategory category,
            OffsetDateTime from,
            OffsetDateTime to) {
        ScopeType scopeType = ScopeType.fromPath(scopeTypeValue);
        validateScopeId(scopeId);
        validateRange(from, to);
        return postRepository.findAll().stream()
                .filter(post -> post.getModerationStatus() != ModerationStatus.HIDDEN_BY_MODERATOR)
                .filter(post -> post.getScopeType() == scopeType && post.getScopeId().equals(scopeId))
                .filter(matchesKeyword(keyword))
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> withinRange(post.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(DiscussionPost::getCreatedAt))
                .map(this::toPostResponse)
                .toList();
    }

    public CommentResponse createComment(UUID postId, CommentRequest request, String authorHandle) {
        DiscussionPost post = requireVisiblePost(postId);
        DiscussionComment parent = null;
        if (request.parentCommentId() != null) {
            parent = requireComment(postId, request.parentCommentId());
            if (!isVisible(parent)) {
                throw new ResourceNotFoundException("Parent comment not found.");
            }
        }

        DiscussionComment comment = new DiscussionComment();
        comment.setPost(post);
        comment.setParentComment(parent);
        comment.setMessage(request.message().trim());
        comment.setAuthorHandle(authorHandle);
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(
            UUID postId,
            String keyword,
            String authorHandle,
            OffsetDateTime from,
            OffsetDateTime to) {
        requireVisiblePost(postId);
        validateRange(from, to);
        return commentRepository.findByPost_IdOrderByCreatedAtAscIdAsc(postId).stream()
                .filter(this::isVisible)
                .filter(matchesCommentKeyword(keyword))
                .filter(comment -> matchesAuthor(comment.getAuthorHandle(), authorHandle))
                .filter(comment -> withinRange(comment.getCreatedAt(), from, to))
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommentResponse getComment(UUID postId, UUID commentId) {
        DiscussionComment comment = requireComment(postId, commentId);
        if (!isVisible(comment)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        return toCommentResponse(comment);
    }

    public CommentResponse updateComment(
            UUID postId,
            UUID commentId,
            CommentUpdateRequest request,
            String authorHandle,
            boolean administrator) {
        DiscussionComment comment = requireComment(postId, commentId);
        if (!isVisible(comment)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        ensureCanManage(comment.getAuthorHandle(), authorHandle, administrator);
        comment.setMessage(request.message().trim());
        return toCommentResponse(commentRepository.saveAndFlush(comment));
    }

    public void deleteComment(
            UUID postId, UUID commentId, String authorHandle, boolean administrator) {
        DiscussionComment comment = requireComment(postId, commentId);
        ensureCanManage(comment.getAuthorHandle(), authorHandle, administrator);
        commentRepository.delete(comment);
    }

    public PostResponse moderatePost(UUID postId, ModerationStatus status) {
        DiscussionPost post = requirePost(postId);
        post.setModerationStatus(status);
        return toPostResponse(postRepository.saveAndFlush(post));
    }

    public CommentResponse moderateComment(UUID postId, UUID commentId, ModerationStatus status) {
        DiscussionComment comment = requireComment(postId, commentId);
        comment.setModerationStatus(status);
        return toCommentResponse(commentRepository.saveAndFlush(comment));
    }

    private void applyPostRequest(DiscussionPost post, PostRequest request) {
        ScopeType scopeType;
        String scopeId;
        int scopeCount = 0;
        scopeType = null;
        scopeId = null;
        if (StringUtils.hasText(request.projectId())) {
            scopeCount++;
            scopeType = ScopeType.PROJECT;
            scopeId = request.projectId();
        }
        if (StringUtils.hasText(request.courseId())) {
            scopeCount++;
            scopeType = ScopeType.COURSE;
            scopeId = request.courseId();
        }
        if (StringUtils.hasText(request.labId())) {
            scopeCount++;
            scopeType = ScopeType.LAB;
            scopeId = request.labId();
        }
        if (scopeCount != 1) {
            throw new InvalidRequestException("Exactly one of projectId, courseId, or labId is required.");
        }
        validateScopeId(scopeId);

        List<String> tags = request.tags() == null ? List.of() : request.tags();
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag) || tag.trim().length() > 50) {
                throw new InvalidRequestException("Tags must contain 1 to 50 non-blank characters.");
            }
            normalizedTags.add(tag.trim());
        }

        post.setTitle(request.title().trim());
        post.setBody(request.body().trim());
        post.setCategory(request.category());
        post.setTags(new ArrayList<>(normalizedTags));
        post.setScopeType(scopeType);
        post.setScopeId(scopeId.trim());
    }

    private DiscussionPost requirePost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));
    }

    private DiscussionPost requireVisiblePost(UUID postId) {
        DiscussionPost post = requirePost(postId);
        if (post.getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
            throw new ResourceNotFoundException("Post not found.");
        }
        return post;
    }

    private DiscussionComment requireComment(UUID postId, UUID commentId) {
        DiscussionComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));
        if (!comment.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        return comment;
    }

    private boolean isVisible(DiscussionComment comment) {
        if (comment.getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR
                || comment.getPost().getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
            return false;
        }
        DiscussionComment parent = comment.getParentComment();
        while (parent != null) {
            if (parent.getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
                return false;
            }
            parent = parent.getParentComment();
        }
        return true;
    }

    private Predicate<DiscussionPost> matchesKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return post -> true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return post -> contains(post.getTitle(), normalized)
                || contains(post.getBody(), normalized)
                || post.getTags().stream().anyMatch(tag -> contains(tag, normalized));
    }

    private Predicate<DiscussionComment> matchesCommentKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return comment -> true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return comment -> contains(comment.getMessage(), normalized);
    }

    private boolean matchesScope(DiscussionPost post, ScopeType type, String scopeId) {
        return !StringUtils.hasText(scopeId)
                || (post.getScopeType() == type && post.getScopeId().equalsIgnoreCase(scopeId.trim()));
    }

    private boolean matchesAuthor(String actual, String requested) {
        return !StringUtils.hasText(requested) || actual.equalsIgnoreCase(requested.trim());
    }

    private void ensureCanManage(String contentAuthor, String actor, boolean administrator) {
        if (!administrator && !contentAuthor.equalsIgnoreCase(actor)) {
            throw new AccessDeniedException("Only the author or an administrator can edit or delete this content.");
        }
    }

    private boolean withinRange(OffsetDateTime value, OffsetDateTime from, OffsetDateTime to) {
        return (from == null || !value.isBefore(from)) && (to == null || !value.isAfter(to));
    }

    private boolean contains(String value, String normalizedKeyword) {
        return value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' must be earlier than or equal to 'to'.");
        }
    }

    private void validateScopeId(String scopeId) {
        if (!StringUtils.hasText(scopeId) || scopeId.trim().length() > 100) {
            throw new InvalidRequestException("Scope IDs must contain 1 to 100 non-blank characters.");
        }
    }

    private PostResponse toPostResponse(DiscussionPost post) {
        String projectId = post.getScopeType() == ScopeType.PROJECT ? post.getScopeId() : null;
        String courseId = post.getScopeType() == ScopeType.COURSE ? post.getScopeId() : null;
        String labId = post.getScopeType() == ScopeType.LAB ? post.getScopeId() : null;
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getCategory(),
                List.copyOf(post.getTags()),
                post.getScopeType().pathValue(),
                post.getScopeId(),
                projectId,
                courseId,
                labId,
                post.getAuthorHandle(),
                post.getModerationStatus(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private CommentResponse toCommentResponse(DiscussionComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                comment.getMessage(),
                comment.getAuthorHandle(),
                comment.getModerationStatus(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
