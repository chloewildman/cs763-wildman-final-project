package edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Comment;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByPostIdAndUser(int postId, User user);
}
