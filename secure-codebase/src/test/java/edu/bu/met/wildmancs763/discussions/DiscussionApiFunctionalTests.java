package edu.bu.met.wildmancs763.discussions;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:discussions-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.bootstrap-admin.username=admin",
        "app.bootstrap-admin.password=Bootstrap-admin-password-123"
})
@AutoConfigureMockMvc
@Transactional
class DiscussionApiFunctionalTests {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession userSession;

    @BeforeEach
    void registerAndLoginResearcher() throws Exception {
        userSession = registerAndLogin("researcher", "Researcher-password-123");
    }

    @Test
    void createsPostsAndSupportsScopedSearchAndTimeline() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Lab meeting notes", "Dataset review is Friday",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.scopeType").value("projects"))
                .andExpect(jsonPath("$.projectId").value("project-42"))
                .andExpect(jsonPath("$.moderationStatus").value("active"));

        mockMvc.perform(get("/api/v1/discussions/posts")
                        .param("keyword", "dataset")
                        .param("projectId", "project-42")
                        .param("category", "PROJECT_UPDATE_BLOG")
                        .param("authorHandle", "researcher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/discussions/scopes/projects/project-42/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Lab meeting notes"));
    }

    @Test
    void rejectsBlankPostTitlesAndAmbiguousScopes() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "body": "Announcement body",
                                  "category": "ACADEMIC_ANNOUNCEMENT",
                                  "authorHandle": "faculty",
                                  "projectId": "project-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("title")));

        mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJsonWithTwoScopes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Exactly one")));
    }

    @Test
    void createsNestedCommentsFiltersStreamsAndExportsTextManifest() throws Exception {
        UUID postId = createPostAndGetId();
        UUID rootCommentId = createCommentAndGetId(
                postId, "{\"message\":\"The data looks good\"}");
        MockHttpSession researcherSession = userSession;
        userSession = registerAndLogin("bob", "Bob-account-password-123");
        UUID replyId = createCommentAndGetId(
                postId, "{\"message\":\"Thanks for reviewing\","
                        + "\"parentCommentId\":\"" + rootCommentId + "\"}");
        userSession = researcherSession;

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments", postId)
                        .param("authorHandle", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(replyId.toString()))
                .andExpect(jsonPath("$[0].parentCommentId").value(rootCommentId.toString()));

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments", postId)
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("researcher | The data looks good")))
                .andExpect(content().string(containsString("bob | Thanks for reviewing")));

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCommentId").value(rootCommentId.toString()));

        mockMvc.perform(withCsrf(post("/api/v1/discussions/posts/{postId}/comments", postId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderationHidesCommentBranchesAndPostsUntilReactivated() throws Exception {
        UUID postId = createPostAndGetId();
        UUID rootCommentId = createCommentAndGetId(
                postId, "{\"message\":\"Parent\"}");
        createCommentAndGetId(
                postId, "{\"message\":\"Nested reply\","
                        + "\"parentCommentId\":\"" + rootCommentId + "\"}");

        userSession = login("admin", "Bootstrap-admin-password-123");
        mockMvc.perform(withCsrf(patch("/api/v1/discussions/posts/{postId}/comments/{commentId}/moderation",
                        postId, rootCommentId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"hidden_by_moderator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("hidden_by_moderator"));

        userSession = login("researcher", "Researcher-password-123");
        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        userSession = login("admin", "Bootstrap-admin-password-123");
        mockMvc.perform(withCsrf(patch("/api/v1/discussions/posts/{postId}/moderation", postId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"hidden_by_moderator\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", postId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/discussions/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(withCsrf(patch("/api/v1/discussions/posts/{postId}/moderation", postId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"reported\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("reported"));

        userSession = login("researcher", "Researcher-password-123");
        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", postId))
                .andExpect(status().isOk());
    }

    @Test
    void updatesAndDeletesPostsAndCommentsAndReportsMissingResources() throws Exception {
        UUID postId = createPostAndGetId();
        UUID commentId = createCommentAndGetId(
                postId, "{\"message\":\"Initial response\"}");

        mockMvc.perform(withCsrf(put("/api/v1/discussions/posts/{postId}", postId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "body": "Updated body",
                                  "category": "ACADEMIC_ANNOUNCEMENT",
                                  "tags": ["research"],
                                  "courseId": "course-9"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.courseId").value("course-9"));

        mockMvc.perform(withCsrf(
                        put("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Edited response\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Edited response"));

        mockMvc.perform(withCsrf(
                        delete("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        userSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(withCsrf(delete("/api/v1/discussions/posts/{postId}", postId), userSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", postId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidScopeAndUnsupportedCommentExportFormat() throws Exception {
        mockMvc.perform(get("/api/v1/discussions/scopes/teams/team-1/timeline"))
                .andExpect(status().isBadRequest());

        UUID postId = createPostAndGetId();
        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments", postId)
                        .param("format", "csv"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrationCannotAssignPrivilegedRolesAndAuthorsComeFromLogin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("researcher"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Anonymous post", "Must sign in",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isUnauthorized());

        String postBody = """
                {
                  "title": "Identity check",
                  "body": "The author is supplied by the session.",
                  "category": "PROJECT_UPDATE_BLOG",
                  "authorHandle": "spoofed",
                  "projectId": "project-42"
                }
                """;
        String postResponse = mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorHandle").value("researcher"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID postId = extractId(postResponse);

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student2","password":"Student-account-password-123","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        MockHttpSession studentSession = login("student2", "Student-account-password-123");
        mockMvc.perform(withCsrf(put("/api/v1/discussions/posts/{postId}", postId), studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unauthorized edit",
                                  "body": "This should not be allowed.",
                                  "category": "PROJECT_UPDATE_BLOG",
                                  "projectId": "project-42"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(withCsrf(patch("/api/v1/discussions/posts/{postId}/moderation", postId),
                        studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"hidden_by_moderator\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(withCsrf(post("/api/v1/admin/users"), studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"faculty","password":"Faculty-account-password-123","role":"FACULTY"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyCanModerateAndAdminCanProvisionAccounts() throws Exception {
        UUID postId = createPostAndGetId();
        userSession = login("admin", "Bootstrap-admin-password-123");
        mockMvc.perform(withCsrf(post("/api/v1/admin/users"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"faculty","password":"Faculty-account-password-123","role":"FACULTY"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FACULTY"));

        userSession = login("faculty", "Faculty-account-password-123");
        mockMvc.perform(withCsrf(patch("/api/v1/discussions/posts/{postId}/moderation", postId),
                        userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"reported\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("reported"));
    }

    @Test
    void rejectsWeakPasswordsAndDuplicateUsernames() throws Exception {
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"shortpass\",\"password\":\"too-short\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"RESEARCHER","password":"Another-password-123"}
                                """))
                .andExpect(status().isConflict());
    }

    private UUID createPostAndGetId() throws Exception {
        String response = mockMvc.perform(withCsrf(post("/api/v1/discussions/posts"), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Research update", "Shared project progress",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private UUID createCommentAndGetId(UUID postId, String requestBody) throws Exception {
        String response = mockMvc.perform(withCsrf(
                        post("/api/v1/discussions/posts/{postId}/comments", postId), userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return extractId(response);
    }

    private UUID extractId(String response) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("Response did not contain a resource ID: " + response);
        }
        return UUID.fromString(matcher.group(1));
    }

    private String postJson(String title, String body, String category, String projectId) {
        return """
                {
                  "title": "%s",
                  "body": "%s",
                  "category": "%s",
                  "tags": ["research", "weekly"],
                  "projectId": "%s"
                }
                """.formatted(title, body, category, projectId);
    }

    private String postJsonWithTwoScopes() {
        return """
                {
                  "title": "Announcement",
                  "body": "Body",
                  "category": "ACADEMIC_ANNOUNCEMENT",
                  "projectId": "project-1",
                  "labId": "lab-1"
                }
                """;
    }

    private MockHttpSession registerAndLogin(String username, String password) throws Exception {
        String registration = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
        mockMvc.perform(withCsrf(post("/api/v1/auth/register"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
        return login(username, password);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(withCsrf(post("/api/v1/auth/login"), null)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder withCsrf(
            MockHttpServletRequestBuilder request,
            MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder tokenRequest = get("/api/v1/auth/csrf");
        if (session != null) {
            tokenRequest.session(session);
        }
        MvcResult tokenResult = mockMvc.perform(tokenRequest)
                .andExpect(status().isOk())
                .andReturn();
        Matcher tokenMatcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(tokenResult.getResponse().getContentAsString());
        if (!tokenMatcher.find()) {
            throw new IllegalStateException("CSRF response did not contain a token.");
        }
        String token = tokenMatcher.group(1);
        Cookie cookie = tokenResult.getResponse().getCookie("XSRF-TOKEN");
        request.cookie(cookie).header("X-XSRF-TOKEN", token);
        if (session != null) {
            request.session(session);
        }
        return request;
    }
}
