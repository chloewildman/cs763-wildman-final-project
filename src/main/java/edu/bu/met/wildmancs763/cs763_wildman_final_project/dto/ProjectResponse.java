package edu.bu.met.wildmancs763.cs763_wildman_final_project.dto;

import java.time.Instant;

public record ProjectResponse(
		int projectId,
		String projectTitle,
		int courseId,
		Instant projectCreationTimestamp) {
}
