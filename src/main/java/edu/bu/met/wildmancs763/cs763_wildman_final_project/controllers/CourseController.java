package edu.bu.met.wildmancs763.cs763_wildman_final_project.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.CourseService;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.UserService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Course;
import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;

    public CourseController(CourseService courseService, UserService userService) {
        this.courseService = courseService;
        this.userService = userService;
    }

    @GetMapping
    public List<Course> getCourses(@RequestParam int userId) {
        User user = userService.getUser(userId);
        return courseService.getCourses(user);
    }

    @GetMapping("/{courseId}")
    public Course getCourse(@PathVariable int courseId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return courseService.getCourse(courseId, user);
    }

    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        User professor = userService.getUser(course.getProfessor().getUserId());
        return courseService.createCourse(course, professor);
    }

    @PutMapping("/{courseId}")
    public Course updateCourse(@PathVariable int courseId, @RequestBody Course course, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return courseService.updateCourse(courseId, course, user);
    }

    @DeleteMapping("/{courseId}")
    public void deleteCourse(@PathVariable int courseId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        courseService.deleteCourse(courseId, user);
    }

}
