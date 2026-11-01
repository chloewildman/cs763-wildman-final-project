package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Course;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.CourseRepository;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final AuthorizationService authorizationService;

    public CourseService(CourseRepository courseRepository, AuthorizationService authorizationService) {
        this.courseRepository = courseRepository;
        this.authorizationService = authorizationService;
    }

    public Course getCourse(int courseId, User user) {
        Course course = courseRepository.findById(courseId).orElseThrow();

        authorizationService.requireCourseAccess(user.getUserId(), courseId);

        return course;
    }

    public List<Course> getCourses(User user) {
        return courseRepository.findUserByUserId(user.getUserId());
    }

    public Course createCourse(Course course, User professor) {
        course.setProfessor(professor);
        course.setCourseCreationTimestamp(LocalDateTime.now());

        return courseRepository.save(course);
    }

    public Course updateCourse(int courseId, Course updatedCourse, User user) {

        authorizationService.requireCourseManagementAccess(user, courseId);

        Course course = courseRepository.findById(courseId).orElseThrow();

        course.setCourseName(updatedCourse.getCourseName());
        course.setCourseTitle(updatedCourse.getCourseTitle());

        return courseRepository.save(course);
    }

    public void deleteCourse(int courseId, User user) {

        authorizationService.requireCourseManagementAccess(user, courseId);

        Course course = courseRepository.findById(courseId).orElseThrow();

        courseRepository.delete(course);
    }
}
