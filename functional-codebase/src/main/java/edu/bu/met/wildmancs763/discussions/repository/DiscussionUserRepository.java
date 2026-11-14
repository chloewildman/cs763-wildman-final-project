package edu.bu.met.wildmancs763.discussions.repository;

import edu.bu.met.wildmancs763.discussions.domain.DiscussionUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiscussionUserRepository extends JpaRepository<DiscussionUser, UUID> {

    Optional<DiscussionUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
