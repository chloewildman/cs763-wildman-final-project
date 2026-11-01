package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.dto.ProjectResponse;

@Service
public class ProjectService {

	private static final Set<Integer> DEMO_COURSE_IDS = Set.of(1, 2);
	private static final List<ProjectResponse> PROJECTS = List.of(
			new ProjectResponse(102, "Research planning", 1, Instant.parse("2026-03-01T10:00:00Z")),
			new ProjectResponse(101, "Initial topic selection", 1, Instant.parse("2026-02-01T10:00:00Z")));

	public List<ProjectResponse> getProjects(int courseId) {
		if (!DEMO_COURSE_IDS.contains(courseId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
		}

		return PROJECTS.stream()
				.filter(project -> project.courseId() == courseId)
				.sorted(Comparator.comparing(ProjectResponse::projectCreationTimestamp).reversed())
				.toList();
	}
}
