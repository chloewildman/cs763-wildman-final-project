/*
AI Contribution Weight: 90% 
Human Reviewed, Edited, and Commented: 10%
Reviewed, commented out understanding throughout, reorganized to my preferred format
*/ 
package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentResponse;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostRequest;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostResponse;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.ProjectResponse;

@Service
public class StudentDiscussionService {

	// Service initializes in-memory data storage for demo
	private final Map<Integer, Student> studentsById = new HashMap<>();
	private final Map<String, Student> studentsByUsername = new HashMap<>();
	private final Map<Integer, Course> courses = new HashMap<>();
	private final Map<Integer, Set<Integer>> studentIdsByCourse = new HashMap<>();
	private final Map<Integer, Project> projects = new HashMap<>();
	private final Map<Integer, Post> posts = new HashMap<>();
	private final Map<Integer, Comment> comments = new HashMap<>();
	private int lastPostId;
	private int lastCommentId;

	// Constructor calls on the rest of the demo data
	StudentDiscussionService() {
		resetDemoData();
	}

	synchronized void resetDemoData() {
		// Clears in-memory data to reset the demo
		studentsById.clear();
		studentsByUsername.clear();
		courses.clear();
		studentIdsByCourse.clear();
		projects.clear();
		posts.clear();
		comments.clear();

		//Add students
		addStudent(5, "lgilmore");
		addStudent(6, "ldanes");
		addStudent(7, "sstjames");

		//Add courses
		courses.put(22, new Course(22));
		courses.put(23, new Course(23));

		// Add students to courses
		studentIdsByCourse.put(22, Set.of(5, 7));
		studentIdsByCourse.put(23, Set.of(6));

		// Add projects
		projects.put(43, new Project(43, "Grant Proposal Project", 22,
				Instant.parse("2000-09-01T13:12:00Z")));
		projects.put(44, new Project(44, "Chuppah Project", 23,
				Instant.parse("2003-02-13T09:00:00Z")));

		// Add posts
		posts.put(1, new Post(1, "Reschedule meeting?", 5, 43,
				Instant.parse("2000-09-05T08:57:00Z"),
				"I'm running late! Can we move the meeting to 9:30?"));
		posts.put(2, new Post(2, "Reminder to contribute", 6, 44,
				Instant.parse("2003-02-28T23:46:04Z"),
				"Just a reminder that the chuppah needs to be done by next week. I can't make it alone!"));

		// Add comments
		comments.put(1, new Comment(1, 7, 1, Instant.parse("2000-09-05T09:01:00Z"),
				"Oh gosh that is so like you! (: Happy to reschedule!"));
		comments.put(2, new Comment(2, 5, 1, Instant.parse("2000-09-05T09:05:32Z"),
				"What can I say!"));
		comments.put(3, new Comment(3, 6, 2, Instant.parse("2003-03-05T08:02:12Z"),
				"Nevermind, I did it myself."));

		// Based on IDs in test data, set where the next increment will begin for new posts and comments.
		lastPostId = 2; // changed next to last -- I find this clearer since it is incrementing from the value, not starting at the value
		lastCommentId = 3;
	}

	// Lists all projects for a given course, only if the user can access the course
	// Must be a valid project in a course where the user is assigned (deferred to requireCourseAssignment)
	// Modified for more realistic test
	synchronized List<ProjectResponse> listProjects(int courseId, String username) {
		Student student = requireStudent(username);
		requireCourseAssignment(courseId, username);
		if (!studentIdsByCourse.getOrDefault(courseId, Set.of()).contains(student.userId())) {
			return List.of();
		}
		// Sort the project list according to my specs (reverse chronological order)
		return projects.values().stream()
				.filter(project -> project.courseId() == courseId)
				.sorted(Comparator.comparing(Project::createdAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	/*
	Renamed requireAssignedProject to requireProjectAccess for clarity.
	Renamed requireAssignedPost to requirePostAccess for clarity.
	*/

	// Retrieves a specific project for a given course and user, only if the user can access the project
	// Must be a valid project in a course where the user is assigned (deferred to requireProjectAccess)
	synchronized ProjectResponse getProject(int courseId, int projectId, String username) {
		return toResponse(requireProjectAccess(courseId, projectId, username));
	}

	// Lists all posts for a given project and user, only if the user can access the project
	// Must be a valid project in a course where the user is assigned (deferred to requireProjectAccess)
	synchronized List<PostResponse> listPosts(int courseId, int projectId, String username) {
		requireProjectAccess(courseId, projectId, username);
		// Sort the post list according to my specs (reverse chronological order)
		return posts.values().stream()
				.filter(post -> post.projectId() == projectId)
				.sorted(Comparator.comparing(Post::createdAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	// Gets a specific post for a given project and user, only if the user can access the post
	// Must be a valid post in a course where the user is assigned (deferred to requirePostAccess)
	synchronized PostResponse getPost(int courseId, int projectId, int postId, String username) {
		return toResponse(requirePostAccess(courseId, projectId, postId, username));
	}

	// Lists all comments for a given post and user, only if the user can access the post
	// Must be a valid post in a course where the user is assigned (deferred to requirePostAccess)
	synchronized List<CommentResponse> listComments(int courseId, int projectId, int postId, String username) {
		requirePostAccess(courseId, projectId, postId, username);
		return comments.values().stream()
				.filter(comment -> comment.postId() == postId)
				.sorted(Comparator.comparing(Comment::createdAt))
				.map(this::toResponse)
				.toList();
	}

	// Gets a specific comment for a given post and user, only if the user can access the post
	// Must be a valid comment in a course where the user is assigned (deferred to requireCommentAccess)
	synchronized CommentResponse getComment(int courseId, int projectId, int postId, int commentId, String username) {
		return toResponse(requireCommentAccess(courseId, projectId, postId, commentId, username));
	}

	// Creates a new post for a given project and user, only if the user can access the project, and adds to in-memory storage
	// Must be a valid project in a course where the user is assigned (deferred to requireProjectAccess)
	synchronized PostResponse createPost(int courseId, int projectId, PostRequest request, String username) {
		Student student = requireStudent(username);
		requireProjectAccess(courseId, projectId, username);
		Post post = new Post(++lastPostId, request.postTitle(), student.userId(), projectId, Instant.now(), request.postBody());
		posts.put(post.postId(), post);
		return toResponse(post);
	}

	// Edits an existing post for a given project and user, only if the user can access the post and is the author, and replaces in-memory storage
	// Must be a valid post in a course where the user is assigned (deferred to requirePostAccess)
	synchronized PostResponse editPost(int courseId, int projectId, int postId, PostRequest request, String username) {
		Student student = requireStudent(username);
		Post post = requirePostAccess(courseId, projectId, postId, username);
		if (post.authorId() != student.userId()) {
			throw forbidden("Students may edit only their own posts");
		}
		Post updated = new Post(post.postId(), request.postTitle(), post.authorId(), post.projectId(), post.createdAt(), request.postBody());
		posts.put(postId, updated);
		return toResponse(updated);
	}

	// Creates a new comment for a given post and user, only if the user can access the post, and adds to in-memory storage
	// Must be a valid post in a course where the user is assigned (deferred to requirePostAccess)
	synchronized CommentResponse createComment(int courseId, int projectId, int postId, String body, String username) {
		Student student = requireStudent(username);
		requirePostAccess(courseId, projectId, postId, username);
		Comment comment = new Comment(++lastCommentId, student.userId(), postId, Instant.now(), body);
		comments.put(comment.commentId(), comment);
		return toResponse(comment);
	}

	// Add a new student to in-memory storage
	private void addStudent(int userId, String username) {
		Student student = new Student(userId, username);
		studentsById.put(userId, student);
		studentsByUsername.put(username, student);
	}

	// Validate a student exists by username
	private Student requireStudent(String username) {
		Student student = studentsByUsername.get(username);
		if (student == null) {
			throw forbidden("No demo student is associated with this account");
		}
		return student;
	}

	// Validate a course exists by courseId
	private Course requireCourse(int courseId) {
		Course course = courses.get(courseId);
		if (course == null) {
			throw notFound("Course not found");
		}
		return course;
	}

	// requireAssignedProject() was unclear in that it suggested there was a project-assignment mechanism, which there currently isn't.
	// Created requireCourseAssignment() to be used for checking course assignment.
	private void requireCourseAssignment(int courseId, String username) {
		requireCourse(courseId);
		Student student = requireStudent(username);

		if(!studentIdsByCourse.getOrDefault(courseId, Set.of()).contains(student.userId())) {
			throw forbidden("Student is not assigned to this course");
		}
	}

	// Validate that a project exists and is associated with the given course, and that the student is assigned to the course
	private Project requireProjectAccess(int courseId, int projectId, String username) {
		requireCourse(courseId);
		requireCourseAssignment(courseId, username);
		Project project = projects.get(projectId);
		if (project == null || project.courseId() != courseId) {
			throw notFound("Project not found in this course");
		}
		return project;
	}

	// Validate that a post exists and is associated with the given project, and that the student is assigned to the course
	private Post requirePostAccess(int courseId, int projectId, int postId, String username) {
		requireProjectAccess(courseId, projectId, username);
		Post post = posts.get(postId);
		if (post == null || post.projectId() != projectId) {
			throw notFound("Post not found in this project");
		}
		return post;
	}

	// Created this for consistency in how other validity checks are performed
	// Validate that a comment exists and is associated with the given post, and that the student is assigned to the course
	private Comment requireCommentAccess(int courseId, int projectId, int postId, int commentId, String username) {
		requirePostAccess(courseId, projectId, postId, username);
		Comment comment = comments.get(commentId);
		if (comment == null || comment.postId() != postId) {
			throw notFound("Comment not found in this post");
		}
		return comment;
	}

	// Methods below are helpers for generating HTTP error responses
	private static ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private static ResponseStatusException forbidden(String message) {
		return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
	}

	// Methods below convert object to a DTO response (ex: Project to ProjectResponse)
	private ProjectResponse toResponse(Project project) {
		return new ProjectResponse(project.projectId(), project.title(), project.courseId(), project.createdAt());
	}

	private PostResponse toResponse(Post post) {
		return new PostResponse(post.postId(), post.title(), post.authorId(), post.projectId(), post.createdAt(), post.body());
	}

	private CommentResponse toResponse(Comment comment) {
		return new CommentResponse(comment.commentId(), comment.authorId(), comment.postId(), comment.createdAt(), comment.body());
	}

	// Records used below in place of database entities, suitable for in-memory storage, but would need to be changed to connect to a DB
	private record Student(int userId, String username) {
	}

	private record Course(int courseId) {
	}

	private record Project(int projectId, String title, int courseId, Instant createdAt) {
	}

	private record Post(int postId, String title, int authorId, int projectId, Instant createdAt, String body) {
	}

	private record Comment(int commentId, int authorId, int postId, Instant createdAt, String body) {
	}
}
