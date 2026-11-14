package edu.bu.met.wildmancs763.discussions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DiscussionApiFunctionalTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsPostsAndSupportsScopedSearchAndTimeline() throws Exception {
        mockMvc.perform(post("/api/v1/discussions/posts")
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
        mockMvc.perform(post("/api/v1/discussions/posts")
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

        mockMvc.perform(post("/api/v1/discussions/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJsonWithTwoScopes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Exactly one")));
    }

    @Test
    void createsNestedCommentsFiltersStreamsAndExportsTextManifest() throws Exception {
        UUID postId = createPostAndGetId();
        UUID rootCommentId = createCommentAndGetId(
                postId, "{\"message\":\"The data looks good\",\"authorHandle\":\"alice\"}");
        UUID replyId = createCommentAndGetId(
                postId, "{\"message\":\"Thanks for reviewing\",\"authorHandle\":\"bob\","
                        + "\"parentCommentId\":\"" + rootCommentId + "\"}");

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
                .andExpect(content().string(containsString("alice | The data looks good")))
                .andExpect(content().string(containsString("bob | Thanks for reviewing")));

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCommentId").value(rootCommentId.toString()));

        mockMvc.perform(post("/api/v1/discussions/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \",\"authorHandle\":\"alice\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderationHidesCommentBranchesAndPostsUntilReactivated() throws Exception {
        UUID postId = createPostAndGetId();
        UUID rootCommentId = createCommentAndGetId(
                postId, "{\"message\":\"Parent\",\"authorHandle\":\"alice\"}");
        createCommentAndGetId(
                postId, "{\"message\":\"Nested reply\",\"authorHandle\":\"bob\","
                        + "\"parentCommentId\":\"" + rootCommentId + "\"}");

        mockMvc.perform(patch("/api/v1/discussions/posts/{postId}/comments/{commentId}/moderation",
                        postId, rootCommentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"hidden_by_moderator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("hidden_by_moderator"));

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/v1/discussions/posts/{postId}/moderation", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"hidden_by_moderator\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", postId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/discussions/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/v1/discussions/posts/{postId}/moderation", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"reported\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("reported"));

        mockMvc.perform(get("/api/v1/discussions/posts/{postId}", postId))
                .andExpect(status().isOk());
    }

    @Test
    void updatesAndDeletesPostsAndCommentsAndReportsMissingResources() throws Exception {
        UUID postId = createPostAndGetId();
        UUID commentId = createCommentAndGetId(
                postId, "{\"message\":\"Initial response\",\"authorHandle\":\"alice\"}");

        mockMvc.perform(put("/api/v1/discussions/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "body": "Updated body",
                                  "category": "ACADEMIC_ANNOUNCEMENT",
                                  "authorHandle": "researcher",
                                  "tags": ["research"],
                                  "courseId": "course-9"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.courseId").value("course-9"));

        mockMvc.perform(put("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Edited response\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Edited response"));

        mockMvc.perform(delete("/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/discussions/posts/{postId}", postId))
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

    private UUID createPostAndGetId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/discussions/posts")
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
        String response = mockMvc.perform(post("/api/v1/discussions/posts/{postId}/comments", postId)
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
                  "authorHandle": "researcher",
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
                  "authorHandle": "faculty",
                  "projectId": "project-1",
                  "labId": "lab-1"
                }
                """;
    }
}
