package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentResponse;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.CommentUpdateRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.ModerationRequest;
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
import edu.bu.met.wildmancs763.discussions.security.CallerIdentity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_LIST_RESULTS = 500;

    private final DiscussionPostRepository postRepository;
    private final DiscussionCommentRepository commentRepository;
    private final LocalMembershipRegistry membershipRegistry;
    private final InputSanitizer sanitizer;
    private final FieldEncryptionService encryptionService;

    public DiscussionService(
            DiscussionPostRepository postRepository,
            DiscussionCommentRepository commentRepository,
            LocalMembershipRegistry membershipRegistry,
            InputSanitizer sanitizer,
            FieldEncryptionService encryptionService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.membershipRegistry = membershipRegistry;
        this.sanitizer = sanitizer;
        this.encryptionService = encryptionService;
    }

    public PostResponse createPost(PostRequest request, Authentication caller) {
        DiscussionPost post = new DiscussionPost();
        applyPostRequest(post, request, caller);
        String subject = CallerIdentity.subject(caller);
        post.setAuthorHandle(subject);
        post.setOwnerSub(subject);
        return toPostResponse(postRepository.save(post), caller);
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
            OffsetDateTime to,
            Authentication caller) {
        validateRange(from, to);
        validateFilter(keyword, "keyword");
        validateFilter(projectId, "projectId");
        validateFilter(courseId, "courseId");
        validateFilter(labId, "labId");
        validateFilter(authorHandle, "authorHandle");
        return postRepository.findAll().stream()
                .filter(post -> canReadPost(post, caller))
                .filter(matchesKeyword(keyword))
                .filter(post -> matchesScope(post, ScopeType.PROJECT, projectId))
                .filter(post -> matchesScope(post, ScopeType.COURSE, courseId))
                .filter(post -> matchesScope(post, ScopeType.LAB, labId))
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> matchesAuthor(post.getAuthorHandle(), authorHandle))
                .filter(post -> withinRange(post.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(DiscussionPost::getCreatedAt).reversed())
                .limit(MAX_LIST_RESULTS)
                .map(post -> toPostResponse(post, caller))
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId, Authentication caller) {
        return toPostResponse(requireReadablePost(postId, caller), caller);
    }

    public PostResponse updatePost(
            UUID postId, PostRequest request, Authentication caller) {
        DiscussionPost post = requireReadablePost(postId, caller);
        ensureCanManage(post.getOwnerSub(), caller);
        applyPostRequest(post, request, caller);
        markAdminAction(post, caller);
        return toPostResponse(postRepository.saveAndFlush(post), caller);
    }

    public void deletePost(UUID postId, Authentication caller) {
        DiscussionPost post = requireReadablePost(postId, caller);
        ensureCanManage(post.getOwnerSub(), caller);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> scopeTimeline(
            String scopeTypeValue,
            String scopeId,
            String keyword,
            PostCategory category,
            OffsetDateTime from,
            OffsetDateTime to,
            Authentication caller) {
        ScopeType scopeType = ScopeType.fromPath(scopeTypeValue);
        String normalizedScopeId = sanitizer.clean(scopeId);
        validateScopeId(normalizedScopeId);
        validateRange(from, to);
        validateFilter(keyword, "keyword");
        if (!canAccessScope(scopeType, normalizedScopeId, caller)) {
            throw new ResourceNotFoundException("Timeline not found.");
        }
        return postRepository.findAll().stream()
                .filter(post -> canReadPost(post, caller))
                .filter(post -> post.getScopeType() == scopeType
                        && post.getScopeId().equalsIgnoreCase(normalizedScopeId))
                .filter(matchesKeyword(keyword))
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> withinRange(post.getCreatedAt(), from, to))
                .sorted(Comparator.comparing(DiscussionPost::getCreatedAt))
                .limit(MAX_LIST_RESULTS)
                .map(post -> toPostResponse(post, caller))
                .toList();
    }

    public CommentResponse createComment(
            UUID postId, CommentRequest request, Authentication caller) {
        DiscussionPost post = requireReadablePost(postId, caller);
        DiscussionComment parent = null;
        if (request.parentCommentId() != null) {
            parent = requireComment(postId, request.parentCommentId());
            if (!canReadComment(parent, caller)) {
                throw new ResourceNotFoundException("Parent comment not found.");
            }
        }

        String message = sanitizer.clean(request.message());
        if (!StringUtils.hasText(message)) {
            throw new InvalidRequestException("Message must contain visible text.");
        }
        DiscussionComment comment = new DiscussionComment();
        comment.setPost(post);
        comment.setParentComment(parent);
        comment.setMessage(message);
        comment.setAuthorHandle(CallerIdentity.subject(caller));
        comment.setOwnerSub(CallerIdentity.subject(caller));
        return toCommentResponse(commentRepository.save(comment), caller);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(
            UUID postId,
            String keyword,
            String authorHandle,
            OffsetDateTime from,
            OffsetDateTime to,
            Authentication caller) {
        requireReadablePost(postId, caller);
        validateRange(from, to);
        validateFilter(keyword, "keyword");
        validateFilter(authorHandle, "authorHandle");
        return commentRepository.findByPost_IdOrderByCreatedAtAscIdAsc(postId).stream()
                .filter(comment -> canReadComment(comment, caller))
                .filter(matchesCommentKeyword(keyword))
                .filter(comment -> matchesAuthor(comment.getAuthorHandle(), authorHandle))
                .filter(comment -> withinRange(comment.getCreatedAt(), from, to))
                .limit(MAX_LIST_RESULTS)
                .map(comment -> toCommentResponse(comment, caller))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommentResponse getComment(UUID postId, UUID commentId, Authentication caller) {
        requireReadablePost(postId, caller);
        DiscussionComment comment = requireComment(postId, commentId);
        if (!canReadComment(comment, caller)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        return toCommentResponse(comment, caller);
    }

    public CommentResponse updateComment(
            UUID postId,
            UUID commentId,
            CommentUpdateRequest request,
            Authentication caller) {
        requireReadablePost(postId, caller);
        DiscussionComment comment = requireComment(postId, commentId);
        if (!canReadComment(comment, caller)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        ensureCanManage(comment.getOwnerSub(), caller);
        String message = sanitizer.clean(request.message());
        if (!StringUtils.hasText(message)) {
            throw new InvalidRequestException("Message must contain visible text.");
        }
        comment.setMessage(message);
        markAdminAction(comment, caller);
        return toCommentResponse(commentRepository.saveAndFlush(comment), caller);
    }

    public void deleteComment(UUID postId, UUID commentId, Authentication caller) {
        requireReadablePost(postId, caller);
        DiscussionComment comment = requireComment(postId, commentId);
        if (!canReadComment(comment, caller)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        ensureCanManage(comment.getOwnerSub(), caller);
        commentRepository.delete(comment);
    }

    public PostResponse moderatePost(
            UUID postId, ModerationRequest request, Authentication caller) {
        ensureModerator(caller);
        DiscussionPost post = requireReadablePost(postId, caller);
        applyModeration(post, request, caller);
        markAdminAction(post, caller);
        return toPostResponse(postRepository.saveAndFlush(post), caller);
    }

    public CommentResponse moderateComment(
            UUID postId, UUID commentId, ModerationRequest request, Authentication caller) {
        ensureModerator(caller);
        requireReadablePost(postId, caller);
        DiscussionComment comment = requireComment(postId, commentId);
        if (!canReadComment(comment, caller)) {
            throw new ResourceNotFoundException("Comment not found.");
        }
        applyModeration(comment, request, caller);
        markAdminAction(comment, caller);
        return toCommentResponse(commentRepository.saveAndFlush(comment), caller);
    }

    private void applyPostRequest(
            DiscussionPost post, PostRequest request, Authentication caller) {
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
        scopeId = sanitizer.clean(scopeId);
        validateScopeId(scopeId);
        if (!canAccessScope(scopeType, scopeId, caller)) {
            throw new AccessDeniedException("Scope membership is required.");
        }
        if (request.category() == PostCategory.ACADEMIC_ANNOUNCEMENT
                && !CallerIdentity.isModerator(caller)) {
            throw new AccessDeniedException("Only faculty instructors or administrators can create announcements.");
        }

        String title = sanitizer.clean(request.title());
        String body = sanitizer.clean(request.body());
        if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
            throw new InvalidRequestException("Title and body must contain visible text.");
        }
        List<String> tags = request.tags() == null ? List.of() : request.tags();
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            String cleanTag = sanitizer.clean(tag);
            if (!StringUtils.hasText(cleanTag) || cleanTag.length() > 50) {
                throw new InvalidRequestException("Tags must contain 1 to 50 visible characters.");
            }
            normalizedTags.add(cleanTag);
        }

        post.setTitle(title);
        post.setBody(body);
        post.setCategory(request.category());
        post.setTags(new ArrayList<>(normalizedTags));
        post.setScopeType(scopeType);
        post.setScopeId(scopeId);
    }

    private void applyModeration(
            DiscussionPost post, ModerationRequest request, Authentication caller) {
        applyModerationFields(
                request,
                caller,
                post::setModerationStatus,
                post::setModerationNoteCiphertext,
                post::setModeratedBySub);
    }

    private void applyModeration(
            DiscussionComment comment, ModerationRequest request, Authentication caller) {
        applyModerationFields(
                request,
                caller,
                comment::setModerationStatus,
                comment::setModerationNoteCiphertext,
                comment::setModeratedBySub);
    }

    private void applyModerationFields(
            ModerationRequest request,
            Authentication caller,
            java.util.function.Consumer<ModerationStatus> statusSetter,
            java.util.function.Consumer<String> noteSetter,
            java.util.function.Consumer<String> moderatorSetter) {
        ModerationStatus status = request.status();
        statusSetter.accept(status);
        if (status == ModerationStatus.ACTIVE) {
            noteSetter.accept(null);
            moderatorSetter.accept(null);
            return;
        }
        String note = request.note() == null ? "" : sanitizer.clean(request.note());
        if (!StringUtils.hasText(note)) {
            throw new InvalidRequestException("A moderation note is required for reported or hidden content.");
        }
        if (note.length() > 1000) {
            throw new InvalidRequestException("Moderation notes must be no more than 1000 characters.");
        }
        noteSetter.accept(encryptionService.encrypt(note));
        moderatorSetter.accept(CallerIdentity.subject(caller));
    }

    private DiscussionPost requirePost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));
    }

    private DiscussionPost requireReadablePost(UUID postId, Authentication caller) {
        DiscussionPost post = requirePost(postId);
        if (!canReadPost(post, caller)) {
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

    private boolean canReadPost(DiscussionPost post, Authentication caller) {
        String subject = CallerIdentity.subject(caller);
        if (post.getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
            return post.getOwnerSub().equals(subject) || CallerIdentity.isModerator(caller);
        }
        return canAccessScope(post.getScopeType(), post.getScopeId(), caller);
    }

    private boolean canReadComment(DiscussionComment comment, Authentication caller) {
        String subject = CallerIdentity.subject(caller);
        if (CallerIdentity.isModerator(caller)) {
            return true;
        }
        if (comment.getPost().getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
            return comment.getOwnerSub().equals(subject)
                    || comment.getPost().getOwnerSub().equals(subject);
        }
        DiscussionComment current = comment;
        while (current != null) {
            if (current.getModerationStatus() == ModerationStatus.HIDDEN_BY_MODERATOR) {
                return comment.getOwnerSub().equals(subject)
                        || current.getOwnerSub().equals(subject);
            }
            current = current.getParentComment();
        }
        return canReadPost(comment.getPost(), caller);
    }

    private boolean canAccessScope(
            ScopeType scopeType, String scopeId, Authentication caller) {
        return CallerIdentity.isAdministrator(caller)
                || membershipRegistry.isMember(scopeType, scopeId, CallerIdentity.subject(caller));
    }

    private void ensureCanManage(String ownerSub, Authentication caller) {
        if (!CallerIdentity.isAdministrator(caller)
                && !ownerSub.equals(CallerIdentity.subject(caller))) {
            throw new AccessDeniedException("Only the author or an administrator can edit or delete this content.");
        }
    }

    private void ensureModerator(Authentication caller) {
        if (!CallerIdentity.isModerator(caller)) {
            throw new AccessDeniedException("Faculty instructor or administrator role is required.");
        }
    }

    private void markAdminAction(DiscussionPost post, Authentication caller) {
        if (CallerIdentity.isAdministrator(caller)
                && !post.getOwnerSub().equals(CallerIdentity.subject(caller))) {
            post.setAdminModified(true);
            post.setAdminModifiedBy(CallerIdentity.subject(caller));
        }
    }

    private void markAdminAction(DiscussionComment comment, Authentication caller) {
        if (CallerIdentity.isAdministrator(caller)
                && !comment.getOwnerSub().equals(CallerIdentity.subject(caller))) {
            comment.setAdminModified(true);
            comment.setAdminModifiedBy(CallerIdentity.subject(caller));
        }
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
        if (!StringUtils.hasText(scopeId) || scopeId.length() > 100
                || scopeId.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidRequestException("Scope IDs must contain 1 to 100 valid characters.");
        }
    }

    private void validateFilter(String value, String name) {
        if (value != null && value.length() > MAX_QUERY_LENGTH) {
            throw new InvalidRequestException(name + " must be no more than 100 characters.");
        }
    }

    private PostResponse toPostResponse(DiscussionPost post, Authentication caller) {
        boolean moderator = CallerIdentity.isModerator(caller);
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
                post.getUpdatedAt(),
                post.isAdminModified(),
                post.getAdminModifiedBy(),
                moderator && post.getModerationNoteCiphertext() != null
                        ? encryptionService.decrypt(post.getModerationNoteCiphertext())
                        : null,
                moderator ? post.getModeratedBySub() : null);
    }

    private CommentResponse toCommentResponse(DiscussionComment comment, Authentication caller) {
        boolean moderator = CallerIdentity.isModerator(caller);
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                comment.getMessage(),
                comment.getAuthorHandle(),
                comment.getModerationStatus(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isAdminModified(),
                comment.getAdminModifiedBy(),
                moderator && comment.getModerationNoteCiphertext() != null
                        ? encryptionService.decrypt(comment.getModerationNoteCiphertext())
                        : null,
                moderator ? comment.getModeratedBySub() : null);
    }
}
