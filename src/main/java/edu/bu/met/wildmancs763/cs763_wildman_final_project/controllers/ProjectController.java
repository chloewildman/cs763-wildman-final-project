package edu.bu.met.wildmancs763.cs763_wildman_final_project.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.dto.ProjectResponse;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.services.ProjectService;

@RestController
@RequestMapping("/courses/{courseId}/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public List<ProjectResponse> getProjects(@PathVariable int courseId) {
		return projectService.getProjects(courseId);
	}
}
