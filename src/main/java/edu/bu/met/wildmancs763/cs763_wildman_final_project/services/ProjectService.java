package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Course;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Project;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.ProjectRepository;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.CourseRepository;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;
    private final CourseRepository courseRepository;

    public ProjectService(ProjectRepository projectRepository, AuthorizationService authorizationService, CourseRepository courseRepository) {
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.courseRepository = courseRepository;
    }

    public List<Project> getProjects(int courseId, User user) {
        return projectRepository.getProjects(courseId, user);
    }

    public Project getProject(int projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow();

        int courseId = project.getCourse()
                .getCourseId();

        authorizationService.requireCourseAccess(
                user.getUserId(),
                courseId);

        return project;
    }

    public Project createProject(int courseId, Project project, User user) {

        authorizationService.requireCourseManagementAccess(user, courseId);

        Course course = courseRepository.findById(courseId).orElseThrow();

        project.setCourse(course);
        project.setProjectCreationTimestamp(LocalDateTime.now());

        return projectRepository.save(project);
    }

    public Project updateProject(int projectId, Project updatedProject, User user) {

        Project project = projectRepository.findById(projectId).orElseThrow();

        authorizationService.requireCourseManagementAccess(user, project.getCourse().getCourseId());

        project.setProjectTitle(updatedProject.getProjectTitle());

        return projectRepository.save(project);
    }

    public void deleteProject(int projectId, User user) {

        Project project = projectRepository.findById(projectId).orElseThrow();

        authorizationService.requireCourseManagementAccess(user, project.getCourse().getCourseId());

        projectRepository.delete(project);
    }
}