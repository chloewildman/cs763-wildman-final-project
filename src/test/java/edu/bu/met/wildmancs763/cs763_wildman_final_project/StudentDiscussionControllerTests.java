package edu.bu.met.wildmancs763.cs763_wildman_final_project;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@SpringBootTest(properties = "DEMO_STUDENT_PASSWORD=integration-test-password")
@AutoConfigureMockMvc
class StudentDiscussionControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentDiscussionService discussionService;

	@BeforeEach
	void resetDemoData() {
		discussionService.resetDemoData();
	}

	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanViewAssignedProjectsAndCannotViewAnotherStudentsProject() throws Exception {
		mockMvc.perform(get("/courses/22/projects"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].projectId", is(43)));

		mockMvc.perform(get("/courses/22/projects/43"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courseId", is(22)));

		mockMvc.perform(get("/courses/23/projects/44"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/courses/22/projects/44"))
				.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentSeesAnEmptyProjectListForACourseTheyAreNotAssignedTo() throws Exception {
		mockMvc.perform(get("/courses/23/projects"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanReadPostsAndChronologicallyOrderedCommentsInAnAssignedProject() throws Exception {
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

	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCanCommentOnPostsInAnAssignedProject() throws Exception {
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

	@Test
	@WithMockUser(username = "lgilmore", roles = "STUDENT")
	void studentCannotUseUnlistedAdminProfessorOrMutationOperations() throws Exception {
		mockMvc.perform(get("/users")).andExpect(status().isForbidden());
		mockMvc.perform(get("/users/5")).andExpect(status().isForbidden());
		mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
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
		mockMvc.perform(patch("/courses/22/projects/43/posts/1/hide")).andExpect(status().isForbidden());
		mockMvc.perform(patch("/courses/22/projects/43/posts/1/comments/1/hide"))
				.andExpect(status().isForbidden());
	}

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

	@Test
	void studentCanAuthenticateWithTheConfiguredDemoPassword() throws Exception {
		mockMvc.perform(get("/courses/22/projects")
						.with(httpBasic("lgilmore", "integration-test-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].projectId", is(43)));
	}

	@Test
	void unauthenticatedStudentRouteIsRejected() throws Exception {
		mockMvc.perform(get("/courses/22/projects"))
				.andExpect(status().isUnauthorized());
	}
}
