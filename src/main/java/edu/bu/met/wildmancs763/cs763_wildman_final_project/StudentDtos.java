package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class StudentDtos {

	private StudentDtos() {
	}

	public record ProjectResponse(
			int projectId,
			String projectTitle,
			int courseId,
			Instant projectCreationTimestamp) {
	}

	public record PostResponse(
			int postId,
			String postTitle,
			int authorId,
			int projectId,
			Instant postTimestamp,
			String postBody) {
	}

	public record CommentResponse(
			int commentId,
			int authorId,
			int postId,
			Instant commentTimestamp,
			String commentBody) {
	}

	public record PostRequest(
			@NotBlank @Size(max = 50) String postTitle,
			@NotBlank @Size(max = 2000) String postBody) {
	}

	public record CommentRequest(
			@NotBlank @Size(max = 2000) String commentBody) {
	}
}
