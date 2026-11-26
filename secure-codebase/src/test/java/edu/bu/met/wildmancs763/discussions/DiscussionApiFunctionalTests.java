package edu.bu.met.wildmancs763.discussions;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserProvisionRequest;
import edu.bu.met.wildmancs763.discussions.domain.UserRole;
import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        "app.rate-limit.expensive-min-interval=0s"
})
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class DiscussionApiFunctionalTests {

    private static final TestJwtIssuer TOKEN_ISSUER;
    private static final String FIELD_KEY;

    static {
        try {
            TOKEN_ISSUER = new TestJwtIssuer();
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            FIELD_KEY = Base64.getEncoder().encodeToString(key);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureSecurity(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.jwk-set-uri", TOKEN_ISSUER::jwksUri);
        registry.add("app.jwt.issuer", () -> TestJwtIssuer.ISSUER);
        registry.add("app.jwt.audience", () -> TestJwtIssuer.AUDIENCE);
        registry.add("app.encryption.field-key", () -> FIELD_KEY);
    }

    @AfterAll
    static void stopTestIssuer() {
        TOKEN_ISSUER.close();
    }

    @BeforeEach
    void provisionKnownAccounts() {
        userAccountService.provision(new UserProvisionRequest("alice", UserRole.STUDENT));
        userAccountService.provision(new UserProvisionRequest("bob", UserRole.STUDENT));
        userAccountService.provision(new UserProvisionRequest("mallory", UserRole.STUDENT));
        userAccountService.provision(new UserProvisionRequest("rate-limit-user", UserRole.STUDENT));
        userAccountService.provision(
                new UserProvisionRequest("professor1", UserRole.FACULTY_INSTRUCTOR));
        userAccountService.setPassword("alice", "Setup-Password-123!");
        userAccountService.setPassword("bob", "Setup-Password-123!");
        userAccountService.setPassword("mallory", "Setup-Password-123!");
        userAccountService.setPassword("rate-limit-user", "Setup-Password-123!");
        userAccountService.setPassword("professor1", "Setup-Password-123!");
    }

    @Test
    void createsPostsAndSupportsScopedSearchAndTimeline() throws Exception {
        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Lab meeting notes", "Dataset review is Friday",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.scopeType").value("projects"))
                .andExpect(jsonPath("$.projectId").value("project-42"))
                .andExpect(jsonPath("$.authorHandle").value("alice"))
                .andExpect(jsonPath("$.moderationStatus").value("active"));

        mockMvc.perform(auth(get("/api/v1/discussions/posts"), "alice", "student")
                        .param("keyword", "dataset")
                        .param("projectId", "project-42")
                        .param("category", "PROJECT_UPDATE_BLOG")
                        .param("authorHandle", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(auth(get("/api/v1/discussions/scopes/projects/project-42/timeline"),
                        "alice", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Lab meeting notes"));
    }

    @Test
    void validatesWritesAndNeutralizesMarkup() throws Exception {
        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "body": "Announcement body",
                                  "category": "PROJECT_UPDATE_BLOG",
                                  "projectId": "project-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("title")));

        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJsonWithTwoScopes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Exactly one")));

        String response = mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson(
                                "<img src=x onerror=alert(1)>Safe title",
                                "<script>alert(1)</script><b>Visible text</b>",
                                "PROJECT_UPDATE_BLOG",
                                "project-42")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Safe title"))
                .andExpect(jsonPath("$.body").value("Visible text"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFalse(response.contains("<script"));
        assertFalse(response.contains("onerror"));

        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Only markup","body":"<script>alert(1)</script>",
                                 "category":"PROJECT_UPDATE_BLOG","projectId":"project-42"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsNestedCommentsFiltersStreamsAndExportsTextManifest() throws Exception {
        UUID postId = createPostAndGetId("alice");
        UUID rootCommentId = createCommentAndGetId(
                "alice", postId, "{\"message\":\"The data looks good\"}");
        UUID replyId = createCommentAndGetId(
                "bob", postId, "{\"message\":\"Thanks for reviewing\","
                        + "\"parentCommentId\":\"" + rootCommentId + "\"}");

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments", postId),
                        "bob", "student")
                        .param("authorHandle", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(replyId.toString()))
                .andExpect(jsonPath("$[0].parentCommentId").value(rootCommentId.toString()));

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments", postId),
                        "alice", "student")
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("alice | The data looks good")))
                .andExpect(content().string(containsString("bob | Thanks for reviewing")));

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments/{commentId}",
                        postId, replyId), "alice", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentCommentId").value(rootCommentId.toString()));

        mockMvc.perform(auth(post("/api/v1/discussions/posts/{postId}/comments", postId),
                        "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesModeratedContentAndEncryptsNotesAtRest() throws Exception {
        UUID postId = createPostAndGetId("alice");
        UUID commentId = createCommentAndGetId("alice", postId, "{\"message\":\"Private reply\"}");

        String moderationBody = """
                {"status":"hidden_by_moderator","note":"Reason <script>alert(1)</script>private"}
                """;
        mockMvc.perform(auth(patch("/api/v1/discussions/posts/{postId}/moderation", postId),
                        "admin", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moderationBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationStatus").value("hidden_by_moderator"))
                .andExpect(jsonPath("$.moderationNote").value("Reason private"))
                .andExpect(jsonPath("$.adminModified").value(true))
                .andExpect(jsonPath("$.adminModifiedBy").value("admin"));

        String rawCiphertext = jdbcTemplate.queryForObject(
                "SELECT moderation_note FROM discussion_posts WHERE id = ?",
                String.class,
                postId);
        assertNotNull(rawCiphertext);
        assertTrue(rawCiphertext.startsWith("v1:"));
        assertFalse(rawCiphertext.contains("Reason private"));

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", postId),
                        "alice", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationNote").doesNotExist());
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", postId),
                        "bob", "student"))
                .andExpect(status().isNotFound());

        mockMvc.perform(auth(patch(
                        "/api/v1/discussions/posts/{postId}/comments/{commentId}/moderation",
                        postId, commentId), "professor1", "faculty_instructor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"reported","note":"Needs review"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationNote").value("Needs review"));

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments/{commentId}",
                        postId, commentId), "bob", "student"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments/{commentId}",
                        postId, commentId), "alice", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderationNote").doesNotExist());
    }

    @Test
    void updatesAndDeletesOwnPostsAndCommentsButNotOtherAuthorsContent() throws Exception {
        UUID postId = createPostAndGetId("alice");
        UUID commentId = createCommentAndGetId("alice", postId, "{\"message\":\"Initial response\"}");

        mockMvc.perform(auth(put("/api/v1/discussions/posts/{postId}", postId), "bob", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Unauthorized edit", "Must be refused",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth(put("/api/v1/discussions/posts/{postId}", postId), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Updated title", "Updated body",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));

        mockMvc.perform(auth(put(
                        "/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Edited response\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Edited response"));

        mockMvc.perform(auth(delete(
                        "/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        "alice", "student"))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth(delete("/api/v1/discussions/posts/{postId}", postId),
                        "alice", "student"))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", postId),
                        "alice", "student"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get(
                        "/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        "alice", "student"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesScopeMembershipAndUsesConsistentHiddenNotFoundResponses() throws Exception {
        UUID postId = createPostAndGetId("alice", "project-99");

        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", postId),
                        "bob", "student"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", postId),
                        "mallory", "student"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(put("/api/v1/discussions/posts/{postId}", postId),
                        "bob", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Changed", "Changed", "PROJECT_UPDATE_BLOG", "project-99")))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(delete("/api/v1/discussions/posts/{postId}", postId),
                        "bob", "student"))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/v1/discussions/scopes/projects/project-42/timeline"),
                        "mallory", "student"))
                .andExpect(status().isNotFound());

        mockMvc.perform(auth(get("/api/v1/discussions/posts"), "mallory", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void onlyFacultyOrAdministratorsCanPublishAnnouncementsOrModerate() throws Exception {
        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Course news","body":"A notice","category":"ACADEMIC_ANNOUNCEMENT",
                                 "courseId":"course-9"}
                                """))
                .andExpect(status().isForbidden());

        UUID postId = createPostAndGetId("alice");
        mockMvc.perform(auth(patch("/api/v1/discussions/posts/{postId}/moderation", postId),
                        "bob", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"reported\",\"note\":\"Flagged\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "professor1", "faculty_instructor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Course news","body":"A notice",
                                 "category":"ACADEMIC_ANNOUNCEMENT","courseId":"course-9"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorHandle").value("professor1"));
    }

    @Test
    void administratorsCanProvisionUsersAndOthersCannot() throws Exception {
        mockMvc.perform(auth(post("/api/v1/admin/users"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newstudent\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth(post("/api/v1/admin/users"), "admin", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newstudent\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/admin/users/")))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(auth(get("/api/v1/discussions/posts"), "newstudent", "student"))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(put("/api/v1/auth/password"), "newstudent", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"New-Student-Password-123!\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/v1/discussions/posts"), "newstudent", "student"))
                .andExpect(status().isOk());
    }

    @Test
    void requiresStrongPasswordsWhenAProvisionedUserSetsOne() throws Exception {
        List<String> weakPasswords = List.of(
                "short1!A",
                "alllowercase123!",
                "ALLUPPERCASE123!",
                "NoDigitsHere!!!!!",
                "NoSpecialCharacter123",
                "Contains<Markup>123A",
                "A".repeat(73) + "a1!");
        for (String password : weakPasswords) {
            mockMvc.perform(auth(put("/api/v1/auth/password"), "alice", "student")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"" + password + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(auth(put("/api/v1/auth/password"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Strong-Password-123!\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void onlyLoginIsPublicAndSensitiveEndpointsRequireAdmin() throws Exception {
        mockMvc.perform(auth(post("/api/v1/auth/login"), "alice", "student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"));

        List<MockHttpServletRequestBuilder> privateRequests = List.of(
                get("/api/v1/auth/me"),
                put("/api/v1/auth/password").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/v1/discussions/posts"),
                post("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/v1/auth/csrf"),
                get("/actuator/health"));
        for (MockHttpServletRequestBuilder request : privateRequests) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(auth(get("/api/v1/discussions/posts"), "unlisted-user", "student"))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(post("/api/v1/auth/login"), "unlisted-user", "student"))
                .andExpect(status().isOk());

        mockMvc.perform(auth(get("/actuator/health"), "alice", "student"))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/actuator/health"), "admin", "admin"))
                .andExpect(status().isOk());
    }

    @Test
    void rateLimitsAtFiftyRequestsAndLocksOutForTenSeconds() throws Exception {
        for (int requestNumber = 1; requestNumber <= 50; requestNumber++) {
            mockMvc.perform(auth(get("/api/v1/auth/me"), "rate-limit-user", "student"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(auth(get("/api/v1/auth/me"), "rate-limit-user", "student"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "10"));
        mockMvc.perform(auth(get("/api/v1/auth/me"), "rate-limit-user", "student"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void appliesAdminModificationMarkersToPostsAndComments() throws Exception {
        UUID postId = createPostAndGetId("alice");
        UUID commentId = createCommentAndGetId("alice", postId, "{\"message\":\"Original\"}");

        mockMvc.perform(auth(put("/api/v1/discussions/posts/{postId}", postId), "admin", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Admin-edited post", "Reviewed", "PROJECT_UPDATE_BLOG",
                                "project-42")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminModified").value(true))
                .andExpect(jsonPath("$.adminModifiedBy").value("admin"));

        mockMvc.perform(auth(put(
                        "/api/v1/discussions/posts/{postId}/comments/{commentId}", postId, commentId),
                        "admin", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Admin-edited comment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminModified").value(true))
                .andExpect(jsonPath("$.adminModifiedBy").value("admin"));
    }

    @Test
    void reportsInvalidResourcesAndUnsupportedExportFormats() throws Exception {
        mockMvc.perform(auth(get("/api/v1/discussions/scopes/teams/team-1/timeline"),
                        "alice", "student"))
                .andExpect(status().isBadRequest());

        UUID postId = createPostAndGetId("alice");
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}/comments", postId),
                        "alice", "student")
                        .param("format", "csv"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(get("/api/v1/discussions/posts/{postId}", UUID.randomUUID()),
                        "alice", "student"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesMockIssuerNegativeTokenCasesAndAllowsEs256() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> expired = Map.of(
                "iat", now - 7200, "nbf", now - 7200, "exp", now - 3600);
        Map<String, Object> notYetValid = Map.of("nbf", now + 3600);
        Map<String, Object> expiredTwoMinutes = Map.of(
                "iat", now - 300, "nbf", now - 300, "exp", now - 120);
        Map<String, Object> wrongIssuer = Map.of("iss", "https://wrong-issuer.example");
        Map<String, Object> wrongAudience = Map.of("aud", "wrong-api");
        Map<String, Object> validAudienceArray = Map.of("aud", List.of("other-api", "cs763-api"));
        Map<String, Object> missingExpiry = new HashMap<>();
        missingExpiry.put("exp", null);

        String valid = TOKEN_ISSUER.issue("alice", List.of("student"));
        String validOther = TOKEN_ISSUER.issue("bob", List.of("student"));
        List<TokenCase> cases = new ArrayList<>();
        cases.add(new TokenCase("valid", valid, true));
        cases.add(new TokenCase("valid_other_user", validOther, true));
        cases.add(new TokenCase("expired", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", expired, Map.of()), false));
        cases.add(new TokenCase("not_yet_valid", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", notYetValid, Map.of()), false));
        cases.add(new TokenCase("missing_exp", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", missingExpiry, Map.of()), false));
        cases.add(new TokenCase("wrong_issuer", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", wrongIssuer, Map.of()), false));
        cases.add(new TokenCase("wrong_audience", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", wrongAudience, Map.of()), false));
        cases.add(new TokenCase("valid_aud_array", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", validAudienceArray, Map.of()), true));
        cases.add(new TokenCase("expired_2min", TOKEN_ISSUER.issueWith(
                "alice", List.of("student"), "RS256", expiredTwoMinutes, Map.of()), false));
        cases.add(new TokenCase("alg_none", TOKEN_ISSUER.issueNone(), false));
        cases.add(new TokenCase("hs256_key_confusion", TOKEN_ISSUER.issueWithAlgorithm(
                "HS256", "alice", List.of("admin")), false));
        cases.add(new TokenCase("alg_not_allowed", TOKEN_ISSUER.issueWith(
                "alice", List.of("admin"), "RS256", Map.of(), Map.of("alg", "PS256")), false));
        cases.add(new TokenCase("bad_signature", TOKEN_ISSUER.issueWithBadSignature(
                "alice", List.of("student")), false));
        cases.add(new TokenCase("tampered_payload", TOKEN_ISSUER.tamperPayload(valid), false));
        cases.add(new TokenCase("unknown_kid", TOKEN_ISSUER.issueWithUnknownKey(
                "alice", List.of("student")), false));
        cases.add(new TokenCase("jku_injection", TOKEN_ISSUER.issueWithJkuInjection(), false));
        cases.add(new TokenCase("malformed", TOKEN_ISSUER.malformedToken(), false));
        cases.add(new TokenCase("ES256", TOKEN_ISSUER.issueEc(
                "alice", List.of("student")), true));

        for (TokenCase tokenCase : cases) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .with(bearerToken(tokenCase.token())))
                    .andReturn();
            assertEquals(tokenCase.accepted() ? 200 : 401,
                    result.getResponse().getStatus(),
                    tokenCase.name());
        }
    }

    @Test
    void returnsSameAuthenticationErrorForInvalidTokensAndAuditsActivity(CapturedOutput output)
            throws Exception {
        MvcResult malformed = mockMvc.perform(post("/api/v1/auth/login")
                        .with(bearerToken(TOKEN_ISSUER.malformedToken())))
                .andReturn();
        MvcResult wrongIssuer = mockMvc.perform(post("/api/v1/auth/login")
                        .with(bearerToken(TOKEN_ISSUER.issueWith(
                                "alice", List.of("student"), "RS256",
                                Map.of("iss", "https://wrong.example"), Map.of()))))
                .andReturn();
        assertEquals(401, malformed.getResponse().getStatus());
        assertEquals(401, wrongIssuer.getResponse().getStatus());
        assertEquals(malformed.getResponse().getContentAsString(),
                wrongIssuer.getResponse().getContentAsString());
        assertEquals("{\"error\":\"Authentication required.\"}",
                malformed.getResponse().getContentAsString());

        mockMvc.perform(auth(post("/api/v1/discussions/posts"), "alice", "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Audit record", "A test post",
                                "PROJECT_UPDATE_BLOG", "project-42")))
                .andExpect(status().isCreated());
        assertTrue(output.getAll().contains("audit username=alice action=POST /api/v1/discussions/posts"));
        assertTrue(output.getAll().contains("resourceId="));
        assertTrue(output.getAll().contains("outcome=SUCCESS"));
        assertTrue(output.getAll().contains("timestamp="));
    }

    private UUID createPostAndGetId(String subject) throws Exception {
        return createPostAndGetId(subject, "project-42");
    }

    private UUID createPostAndGetId(String subject, String projectId) throws Exception {
        String response = mockMvc.perform(auth(post("/api/v1/discussions/posts"),
                        subject, "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson("Research update", "Shared project progress",
                                "PROJECT_UPDATE_BLOG", projectId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(response.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }

    private UUID createCommentAndGetId(String subject, UUID postId, String requestBody) throws Exception {
        String response = mockMvc.perform(auth(
                        post("/api/v1/discussions/posts/{postId}/comments", postId), subject, "student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(response.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
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
                  "category": "PROJECT_UPDATE_BLOG",
                  "projectId": "project-1",
                  "labId": "lab-1"
                }
                """;
    }

    private MockHttpServletRequestBuilder auth(
            MockHttpServletRequestBuilder request, String subject, String... roles) {
        return request.with(bearer(subject, roles));
    }

    private RequestPostProcessor bearer(String subject, String... roles) {
        return bearerToken(TOKEN_ISSUER.issue(subject, List.of(roles)));
    }

    private RequestPostProcessor bearerToken(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private record TokenCase(String name, String token, boolean accepted) {
    }
}
