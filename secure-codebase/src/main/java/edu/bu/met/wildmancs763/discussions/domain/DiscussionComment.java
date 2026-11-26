/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "discussion_comments")
public class DiscussionComment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private DiscussionPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private DiscussionComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscussionComment> replies = new ArrayList<>();

    @Lob
    @Column(nullable = false)
    private String message;

    @Column(nullable = false, length = 100)
    private String authorHandle;

    @Column(name = "owner_sub", length = 100)
    private String ownerSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationStatus moderationStatus = ModerationStatus.ACTIVE;

    @Column(name = "moderation_note", length = 2048)
    private String moderationNoteCiphertext;

    @Column(name = "moderated_by_sub", length = 100)
    private String moderatedBySub;

    @Column(name = "admin_modified")
    private Boolean adminModified = false;

    @Column(name = "admin_modified_by", length = 100)
    private String adminModifiedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public DiscussionComment() {
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public DiscussionPost getPost() {
        return post;
    }

    public void setPost(DiscussionPost post) {
        this.post = post;
    }

    public DiscussionComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(DiscussionComment parentComment) {
        this.parentComment = parentComment;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorHandle() {
        return authorHandle;
    }

    public void setAuthorHandle(String authorHandle) {
        this.authorHandle = authorHandle;
    }

    public String getOwnerSub() {
        return ownerSub == null ? authorHandle : ownerSub;
    }

    public void setOwnerSub(String ownerSub) {
        this.ownerSub = ownerSub;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public String getModerationNoteCiphertext() {
        return moderationNoteCiphertext;
    }

    public void setModerationNoteCiphertext(String moderationNoteCiphertext) {
        this.moderationNoteCiphertext = moderationNoteCiphertext;
    }

    public String getModeratedBySub() {
        return moderatedBySub;
    }

    public void setModeratedBySub(String moderatedBySub) {
        this.moderatedBySub = moderatedBySub;
    }

    public boolean isAdminModified() {
        return Boolean.TRUE.equals(adminModified);
    }

    public void setAdminModified(boolean adminModified) {
        this.adminModified = adminModified;
    }

    public String getAdminModifiedBy() {
        return adminModifiedBy;
    }

    public void setAdminModifiedBy(String adminModifiedBy) {
        this.adminModifiedBy = adminModifiedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
