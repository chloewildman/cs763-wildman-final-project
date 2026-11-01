package edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Course;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    boolean userHasCourseAccess(int userId, int courseId);

    List<Course> findUserByUserId(int userId);
    
}
