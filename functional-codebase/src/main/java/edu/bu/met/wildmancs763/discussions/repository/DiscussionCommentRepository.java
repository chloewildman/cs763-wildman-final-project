package edu.bu.met.wildmancs763.discussions.repository;

import edu.bu.met.wildmancs763.discussions.domain.DiscussionComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscussionCommentRepository extends JpaRepository<DiscussionComment, UUID> {

    List<DiscussionComment> findByPost_IdOrderByCreatedAtAscIdAsc(UUID postId);
}
