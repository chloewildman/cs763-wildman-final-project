/*
AI Contribution Weight: 97% 
Human Reviewed and Commented: 3%
Reviewed, commented out understanding throughout, reorganized to my preferred format
*/
package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

// Test-only, not secure password used only for tests where docker is not used
@SpringBootTest(properties = "DEMO_STUDENT_PASSWORD=test-only-password")
@AutoConfigureMockMvc
class StudentDiscussionControllerTests {

	// Prepare to simulate HTTP requests
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentDiscussionService discussionService;

	// Prior to each test, reset the fake data
	@BeforeEach
	void resetDemoData() {
		discussionService.resetDemoData();
	}

	// Test 1: Student cannot view projects for a course they are not assigned to
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCannotViewProjectsForUnassignedCourse() throws Exception {
		mockMvc.perform(get("/courses/23/projects"))
				.andExpect(status().isForbidden());
	}

	// Test 2: A student can retrieve posts and comments from courses they are apart
	// of (and therefore posts & comments in the course), and they are returned in
	// reverse chronological order
	// Renamed for clarification
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanReadPostsAndChronologicallyOrderedCommentsInAnAssignedCourse() throws Exception {
		mockMvc.perform(get("/courses/22/projects/43/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].postId", is(1)));

		mockMvc.perform(get("/courses/22/projects/43/posts/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorId", is(5)));

		mockMvc.perform(get("/courses/22/projects/43/posts/1/comments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].commentId", is(1)))
				.andExpect(jsonPath("$[1].commentId", is(2)));

		mockMvc.perform(get("/courses/22/projects/43/posts/1/comments/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postId", is(1)));
	}

	// Test 3: Student can create a post and then edit a post they authored.
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanCreateAndEditTheirOwnPosts() throws Exception {
		mockMvc.perform(post("/courses/22/projects/43/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"postTitle":"Meeting update","postBody":"The meeting is at 9:30."}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId", is(3)))
				.andExpect(jsonPath("$.authorId", is(5)))
				.andExpect(jsonPath("$.projectId", is(43)));

		mockMvc.perform(put("/courses/22/projects/43/posts/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"postTitle":"Corrected title","postBody":"Corrected details."}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postTitle", is("Corrected title")))
				.andExpect(jsonPath("$.authorId", is(5)));

		mockMvc.perform(get("/courses/22/projects/43/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].postId", is(3)));
	}

	// Test 4: A student cannot edit a post authored by another student (post in
	// test is authored by user ID 5, lgilmore)
	@Test
	@WithMockUser(username = "sstjames", roles = "STUDENT")
	void studentCannotEditAnotherStudentsPost() throws Exception {
		mockMvc.perform(put("/courses/22/projects/43/posts/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"postTitle":"Changed title","postBody":"Changed details."}
						"""))
				.andExpect(status().isForbidden());
	}

	// Test 5: A student can comment on posts that are in a course where they are
	// assigned
	// Renamed for clarification
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanCommentOnPostsInAnAssignedCourse() throws Exception {
		mockMvc.perform(post("/courses/22/projects/43/posts/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"commentBody":"I can join at 9:30."}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.commentId", is(4)))
				.andExpect(jsonPath("$.authorId", is(5)))
				.andExpect(jsonPath("$.postId", is(1)));
	}

	// Test 6: A student cannot access endpoints not explicitly allowed for their
	// role, nor take actions not permitted for their role
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCannotUseUnlistedAdminProfessorOrMutationOperations() throws Exception {
		mockMvc.perform(get("/users")).andExpect(status().isForbidden());
		mockMvc.perform(get("/users/5")).andExpect(status().isForbidden());
		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
		/*
		 * Noticing here that in a real application a student should be able to access a
		 * list of their own courses, wanted to let you know I am aware of this
		 * limitation in the current test setup.
		 * Since the purpose of the assignment is 'hello world', and in the interest of
		 * time, I will keep this forbidden and not update methods, but I made a
		 * clarifying note in spec.md
		 */
		mockMvc.perform(get("/courses")).andExpect(status().isForbidden());
		mockMvc.perform(get("/courses/22")).andExpect(status().isForbidden());
		mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/courses/22/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/courses/22")).andExpect(status().isForbidden());
		mockMvc.perform(delete("/courses/22")).andExpect(status().isForbidden());
		mockMvc.perform(delete("/courses/22/projects/43")).andExpect(status().isForbidden());
		mockMvc.perform(delete("/courses/22/projects/43/posts/1")).andExpect(status().isForbidden());
	}

	// Test 7: A user tries to submit an invalid data (blanks) and receives a bad
	// request
	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentRequestsMustPassBodyValidation() throws Exception {
		mockMvc.perform(post("/courses/22/projects/43/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"postTitle":" ","postBody":"body"}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/courses/22/projects/43/posts/1/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"commentBody":""}
						"""))
				.andExpect(status().isBadRequest());
	}

	// Test 8: A student user can authenticate with the demo password provided
	@Test
	void studentCanAuthenticateWithTheConfiguredDemoPassword() throws Exception {
		mockMvc.perform(get("/courses/22/projects")
				.with(httpBasic("lgilmore", "test-only-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].projectId", is(43)));
	}

	// Test 9: An anonymous user (no WithMockUser, no .with(httpBasic)) attempts to
	// access projects and should be rejected
	@Test
	void unauthenticatedStudentRouteIsRejected() throws Exception {
		mockMvc.perform(get("/courses/22/projects"))
				.andExpect(status().isUnauthorized());
	}
}
