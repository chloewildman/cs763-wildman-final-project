package edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Project;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> getProjects(int courseId, User user);
}
