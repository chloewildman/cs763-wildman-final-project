package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentRequest;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentResponse;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostRequest;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostResponse;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.ProjectResponse;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentDiscussionController {

	private final StudentDiscussionService discussionService;

	public StudentDiscussionController(StudentDiscussionService discussionService) {
		this.discussionService = discussionService;
	}

	@GetMapping("/courses/{courseId}/projects")
	public List<ProjectResponse> listProjects(
			@PathVariable("courseId") int courseId, Principal principal) {
		return discussionService.listProjects(courseId, principal.getName());
	}

	@GetMapping("/courses/{courseId}/projects/{projectId}")
	public ProjectResponse getProject(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			Principal principal) {
		return discussionService.getProject(courseId, projectId, principal.getName());
	}

	@GetMapping("/courses/{courseId}/projects/{projectId}/posts")
	public List<PostResponse> listPosts(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			Principal principal) {
		return discussionService.listPosts(courseId, projectId, principal.getName());
	}

	@PostMapping("/courses/{courseId}/projects/{projectId}/posts")
	public ResponseEntity<PostResponse> createPost(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@Valid @RequestBody PostRequest request,
			Principal principal) {
		PostResponse post = discussionService.createPost(courseId, projectId, request, principal.getName());
		URI location = URI.create("/courses/" + courseId + "/projects/" + projectId
				+ "/posts/" + post.postId());
		return ResponseEntity.created(location).body(post);
	}

	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}")
	public PostResponse getPost(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@PathVariable("postId") int postId,
			Principal principal) {
		return discussionService.getPost(courseId, projectId, postId, principal.getName());
	}

	@PutMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}")
	public PostResponse editPost(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@PathVariable("postId") int postId,
			@Valid @RequestBody PostRequest request,
			Principal principal) {
		return discussionService.editPost(courseId, projectId, postId, request, principal.getName());
	}

	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments")
	public List<CommentResponse> listComments(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@PathVariable("postId") int postId,
			Principal principal) {
		return discussionService.listComments(courseId, projectId, postId, principal.getName());
	}

	@PostMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments")
	public ResponseEntity<CommentResponse> createComment(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@PathVariable("postId") int postId,
			@Valid @RequestBody CommentRequest request,
			Principal principal) {
		CommentResponse comment = discussionService.createComment(
				courseId, projectId, postId, request.commentBody(), principal.getName());
		URI location = URI.create("/courses/" + courseId + "/projects/" + projectId
				+ "/posts/" + postId + "/comments/" + comment.commentId());
		return ResponseEntity.created(location).body(comment);
	}

	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments/{commentId}")
	public CommentResponse getComment(
			@PathVariable("courseId") int courseId,
			@PathVariable("projectId") int projectId,
			@PathVariable("postId") int postId,
			@PathVariable("commentId") int commentId,
			Principal principal) {
		return discussionService.getComment(
				courseId, projectId, postId, commentId, principal.getName());
	}
}
