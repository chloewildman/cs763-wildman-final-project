package edu.bu.met.wildmancs763.discussions.repository;

import edu.bu.met.wildmancs763.discussions.domain.DiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, UUID> {
}
