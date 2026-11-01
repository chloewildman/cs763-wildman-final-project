package edu.bu.met.wildmancs763.cs763_wildman_final_project.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.Project;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.ProjectService;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.UserService;

@RestController
@RequestMapping("/courses/{courseId}/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping
    public List<Project> getProjects(@PathVariable int courseId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return projectService.getProjects(courseId, user);
    }

    @GetMapping("/{projectId}")
    public Project getProject(@PathVariable int projectId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return projectService.getProject(projectId, user);
    }

    @PostMapping
    public Project createProject(@PathVariable int courseId, @RequestBody Project project, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return projectService.createProject(courseId, project, user);
    }

    @PutMapping("/{projectId}")
    public Project updateProject(@PathVariable int projectId, @RequestBody Project project, @RequestParam int userId) {
        User user = userService.getUser(userId);
        return projectService.updateProject(projectId, project, user);
    }

    @DeleteMapping("/{projectId}")
    public void deleteProject(@PathVariable int projectId, @RequestParam int userId) {
        User user = userService.getUser(userId);
        projectService.deleteProject(projectId, user);
    }

}