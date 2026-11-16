/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.repository;

import edu.bu.met.wildmancs763.discussions.domain.DiscussionUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiscussionUserRepository extends JpaRepository<DiscussionUser, UUID> {

    Optional<DiscussionUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
