/*
AI Contribution Weight: 99% 
Human Reviewed and Commented: 1%
Reviewed, commented out understanding throughout, reorganized to my preferred format
*/ 
package edu.bu.met.wildmancs763.cs763_wildman_final_project;

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

import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentRequest;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentResponse;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostRequest;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostResponse;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.ProjectResponse;

@RestController
public class StudentDiscussionController {

	private final StudentDiscussionService discussionService;

	public StudentDiscussionController(StudentDiscussionService discussionService) {
		this.discussionService = discussionService;
	}

	// Controls GET /projects endpoint, course Id is provided in the given path, courseId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects")
	// Uses ProjectResponse DTO to return the list of projects for the specified course.
	public List<ProjectResponse> listProjects(@PathVariable("courseId") int courseId, Principal principal) {
		return discussionService.listProjects(courseId, principal.getName());
	}

	// Controls GET /projects/{projectId} endpoint, course Id and project Id are provided in the given path, courseId, projectId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects/{projectId}")
	// Uses ProjectResponse DTO to return the details of the specified project.
	public ProjectResponse getProject(@PathVariable("courseId") int courseId,@PathVariable("projectId") int projectId,Principal principal) {
		return discussionService.getProject(courseId, projectId, principal.getName());
	}

	// Controls GET /projects/{projectId}/posts endpoint, course Id and project Id are provided in the given path, courseId, projectId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects/{projectId}/posts")
	// Uses PostResponse DTO to return the list of posts for the specified project.
	public List<PostResponse> listPosts(@PathVariable("courseId") int courseId,@PathVariable("projectId") int projectId,Principal principal) {
		return discussionService.listPosts(courseId, projectId, principal.getName());
	}

	// Controls POST /projects/{projectId}/posts endpoint, course Id and project Id are provided in the given path, courseId, projectId, post request and authenticated user (Spring security) are passed to service layer
	@PostMapping("/courses/{courseId}/projects/{projectId}/posts")
	// Uses the PostResponse DTO to return the details of the newly created post.
	public ResponseEntity<PostResponse> createPost(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @Valid @RequestBody PostRequest request, Principal principal) {
		PostResponse post = discussionService.createPost(courseId, projectId, request, principal.getName());
		URI location = URI.create("/courses/" + courseId + "/projects/" + projectId + "/posts/" + post.postId());
		// Returns non-generic HTTP response with 201 Created status and location header pointing to the newly created post
		return ResponseEntity.created(location).body(post);
	}

	// Controls GET /posts/{postId} endpoint, course Id, project Id and post Id are provided in the given path, courseId, projectId, postId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}")
	// Uses the PostResponse DTO to return the details of the specified post.
	public PostResponse getPost(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @PathVariable("postId") int postId, Principal principal) {
		return discussionService.getPost(courseId, projectId, postId, principal.getName());
	}

	// Controls PUT /posts/{postId} endpoint, course Id, project Id and post Id are provided in the given path, courseId, projectId, postId, post request and authenticated user (Spring security) are passed to service layer
	@PutMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}")
	// Uses the PostResponse DTO to return the details of the updated post.
	public PostResponse editPost(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @PathVariable("postId") int postId, @Valid @RequestBody PostRequest request, Principal principal) {
		return discussionService.editPost(courseId, projectId, postId, request, principal.getName());
	}

	// Controls GET /posts/{postId}/comments endpoint, course Id, project Id and post Id are provided in the given path, courseId, projectId, postId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments")
	// Uses the CommentResponse DTO to return the list of comments for the specified post.
	public List<CommentResponse> listComments(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @PathVariable("postId") int postId, Principal principal) {
		return discussionService.listComments(courseId, projectId, postId, principal.getName());
	}

	// Controls POST /posts/{postId}/comments endpoint, course Id, project Id and post Id are provided in the given path, courseId, projectId, postId, comment request and authenticated user (Spring security) are passed to service layer
	@PostMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments")
	// Uses the CommentResponse DTO to return the details of the newly created comment.
	public ResponseEntity<CommentResponse> createComment(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @PathVariable("postId") int postId, @Valid @RequestBody CommentRequest request, Principal principal) {
		CommentResponse comment = discussionService.createComment(courseId, projectId, postId, request.commentBody(), principal.getName());
		URI location = URI.create("/courses/" + courseId + "/projects/" + projectId + "/posts/" + postId + "/comments/" + comment.commentId());
		// Returns non-generic HTTP response with 201 Created status and location header pointing to the newly created comment
		return ResponseEntity.created(location).body(comment);
	}

	// Controls GET /posts/{postId}/comments/{commentId} endpoint, course Id, project Id, post Id and comment Id are provided in the given path, courseId, projectId, postId, commentId and authenticated user (Spring security) are passed to service layer
	@GetMapping("/courses/{courseId}/projects/{projectId}/posts/{postId}/comments/{commentId}")
	// Uses the CommentResponse DTO to return the details of the specified comment.
	public CommentResponse getComment(@PathVariable("courseId") int courseId, @PathVariable("projectId") int projectId, @PathVariable("postId") int postId, @PathVariable("commentId") int commentId, Principal principal) {
		return discussionService.getComment(courseId, projectId, postId, commentId, principal.getName());
	}
}
