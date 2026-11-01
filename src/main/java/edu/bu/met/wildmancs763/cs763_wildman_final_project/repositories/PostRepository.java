package edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
    List<Post> findByProjectIdAndUserId(int projectId, int userId);
}
