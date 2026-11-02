package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.CommentResponse;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostRequest;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.PostResponse;
import static edu.bu.met.wildmancs763.cs763_wildman_final_project.StudentDtos.ProjectResponse;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentDiscussionService {

	private final Map<Integer, Student> studentsById = new HashMap<>();
	private final Map<String, Student> studentsByUsername = new HashMap<>();
	private final Map<Integer, Course> courses = new HashMap<>();
	private final Map<Integer, Set<Integer>> studentIdsByCourse = new HashMap<>();
	private final Map<Integer, Project> projects = new HashMap<>();
	private final Map<Integer, Post> posts = new HashMap<>();
	private final Map<Integer, Comment> comments = new HashMap<>();
	private int nextPostId;
	private int nextCommentId;

	StudentDiscussionService() {
		resetDemoData();
	}

	synchronized void resetDemoData() {
		studentsById.clear();
		studentsByUsername.clear();
		courses.clear();
		studentIdsByCourse.clear();
		projects.clear();
		posts.clear();
		comments.clear();

		addStudent(5, "lgilmore");
		addStudent(6, "ldanes");
		addStudent(7, "sstjames");

		courses.put(22, new Course(22));
		courses.put(23, new Course(23));
		studentIdsByCourse.put(22, Set.of(5, 7));
		studentIdsByCourse.put(23, Set.of(6));

		projects.put(43, new Project(43, "Grant Proposal Project", 22,
				Instant.parse("2000-09-01T13:12:00Z")));
		projects.put(44, new Project(44, "Chuppah Project", 23,
				Instant.parse("2003-02-13T09:00:00Z")));

		posts.put(1, new Post(1, "Reschedule meeting?", 5, 43,
				Instant.parse("2000-09-05T08:57:00Z"),
				"I'm running late! Can we move the meeting to 9:30?"));
		posts.put(2, new Post(2, "Reminder to contribute", 6, 44,
				Instant.parse("2003-02-28T23:46:04Z"),
				"Just a reminder that the chuppah needs to be done by next week. I can't make it alone!"));

		comments.put(1, new Comment(1, 7, 1, Instant.parse("2000-09-05T09:01:00Z"),
				"Oh gosh that is so like you! (: Happy to reschedule!"));
		comments.put(2, new Comment(2, 5, 1, Instant.parse("2000-09-05T09:05:32Z"),
				"What can I say!"));
		comments.put(3, new Comment(3, 6, 2, Instant.parse("2003-03-05T08:02:12Z"),
				"Nevermind, I did it myself."));

		nextPostId = 2;
		nextCommentId = 3;
	}

	synchronized List<ProjectResponse> listProjects(int courseId, String username) {
		Student student = requireStudent(username);
		requireCourse(courseId);
		if (!studentIdsByCourse.getOrDefault(courseId, Set.of()).contains(student.userId())) {
			return List.of();
		}
		return projects.values().stream()
				.filter(project -> project.courseId() == courseId)
				.sorted(Comparator.comparing(Project::createdAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	synchronized ProjectResponse getProject(int courseId, int projectId, String username) {
		return toResponse(requireAssignedProject(courseId, projectId, username));
	}

	synchronized List<PostResponse> listPosts(int courseId, int projectId, String username) {
		requireAssignedProject(courseId, projectId, username);
		return posts.values().stream()
				.filter(post -> post.projectId() == projectId)
				.sorted(Comparator.comparing(Post::createdAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	synchronized PostResponse getPost(int courseId, int projectId, int postId, String username) {
		return toResponse(requireAssignedPost(courseId, projectId, postId, username));
	}

	synchronized List<CommentResponse> listComments(
			int courseId, int projectId, int postId, String username) {
		requireAssignedPost(courseId, projectId, postId, username);
		return comments.values().stream()
				.filter(comment -> comment.postId() == postId)
				.sorted(Comparator.comparing(Comment::createdAt))
				.map(this::toResponse)
				.toList();
	}

	synchronized CommentResponse getComment(
			int courseId, int projectId, int postId, int commentId, String username) {
		requireAssignedPost(courseId, projectId, postId, username);
		Comment comment = comments.get(commentId);
		if (comment == null || comment.postId() != postId) {
			throw notFound("Comment not found");
		}
		return toResponse(comment);
	}

	synchronized PostResponse createPost(
			int courseId, int projectId, PostRequest request, String username) {
		Student student = requireStudent(username);
		requireAssignedProject(courseId, projectId, username);
		Post post = new Post(++nextPostId, request.postTitle(), student.userId(),
				projectId, Instant.now(), request.postBody());
		posts.put(post.postId(), post);
		return toResponse(post);
	}

	synchronized PostResponse editPost(
			int courseId, int projectId, int postId, PostRequest request, String username) {
		Student student = requireStudent(username);
		Post post = requireAssignedPost(courseId, projectId, postId, username);
		if (post.authorId() != student.userId()) {
			throw forbidden("Students may edit only their own posts");
		}
		Post updated = new Post(post.postId(), request.postTitle(), post.authorId(),
				post.projectId(), post.createdAt(), request.postBody());
		posts.put(postId, updated);
		return toResponse(updated);
	}

	synchronized CommentResponse createComment(
			int courseId, int projectId, int postId, String body, String username) {
		Student student = requireStudent(username);
		requireAssignedPost(courseId, projectId, postId, username);
		Comment comment = new Comment(++nextCommentId, student.userId(),
				postId, Instant.now(), body);
		comments.put(comment.commentId(), comment);
		return toResponse(comment);
	}

	private void addStudent(int userId, String username) {
		Student student = new Student(userId, username);
		studentsById.put(userId, student);
		studentsByUsername.put(username, student);
	}

	private Student requireStudent(String username) {
		Student student = studentsByUsername.get(username);
		if (student == null) {
			throw forbidden("No demo student is associated with this account");
		}
		return student;
	}

	private Course requireCourse(int courseId) {
		Course course = courses.get(courseId);
		if (course == null) {
			throw notFound("Course not found");
		}
		return course;
	}

	private Project requireAssignedProject(int courseId, int projectId, String username) {
		requireCourse(courseId);
		Project project = projects.get(projectId);
		if (project == null || project.courseId() != courseId) {
			throw notFound("Project not found in this course");
		}
		Student student = requireStudent(username);
		if (!studentIdsByCourse.getOrDefault(courseId, Set.of()).contains(student.userId())) {
			throw forbidden("This project is not assigned to the student");
		}
		return project;
	}

	private Post requireAssignedPost(int courseId, int projectId, int postId, String username) {
		requireAssignedProject(courseId, projectId, username);
		Post post = posts.get(postId);
		if (post == null || post.projectId() != projectId) {
			throw notFound("Post not found in this project");
		}
		return post;
	}

	private static ResponseStatusException notFound(String message) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
	}

	private static ResponseStatusException forbidden(String message) {
		return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
	}

	private ProjectResponse toResponse(Project project) {
		return new ProjectResponse(project.projectId(), project.title(),
				project.courseId(), project.createdAt());
	}

	private PostResponse toResponse(Post post) {
		return new PostResponse(post.postId(), post.title(), post.authorId(),
				post.projectId(), post.createdAt(), post.body());
	}

	private CommentResponse toResponse(Comment comment) {
		return new CommentResponse(comment.commentId(), comment.authorId(),
				comment.postId(), comment.createdAt(), comment.body());
	}

	private record Student(int userId, String username) {
	}

	private record Course(int courseId) {
	}

	private record Project(int projectId, String title, int courseId, Instant createdAt) {
	}

	private record Post(
			int postId, String title, int authorId, int projectId, Instant createdAt, String body) {
	}

	private record Comment(
			int commentId, int authorId, int postId, Instant createdAt, String body) {
	}
}
