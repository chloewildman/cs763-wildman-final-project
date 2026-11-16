# SAST Triage Report

## AI Deliverable Metadata Block
This document was written with...
AI contribution: 0%
Human contribution: 100%

## Scan findings
### Semgrep
<b>No findings</b>

│ Scan Summary │

✅ Scan completed successfully.

 • Findings: 0 (0 blocking)

 • Rules run: 172

 • Targets scanned: 29

 • Parsed lines: ~100.0%

 • Scan skipped: 

   ◦ Files matching .semgrepignore patterns: 1

 • Scan was limited to files tracked by git

 • For a detailed list of skipped files and lines, run semgrep with the --verbose flag

Ran 172 rules on 29 files: 0 findings.

### Trivy
<b>No findings</b>

#### Repository scan
Report Summary

| Target | Type | Vulnerabilities | Secrets |
|--------|------|-----------------|---------|
| pom.xml | pom  | 0 | - |
  
  Legend:
  - '-': Not scanned
  - '0': Clean (no security findings detected)

#### Docker image scan
| Target | Type | Vulnerabilities | Secrets |
|--------|------|-----------------|---------|
|cs763-student-demo:23295e5f07955ec7688726af6a9dce7b1442c1c1 (ubuntu 22.04) | ubuntu | 0 | - |
| app/app.jar | jar | 0 | - |

### SonarQube
<b>1 finding</b>

#### Make sure creating this cookie without the "HttpOnly" flag is safe

<b>CWE:</b> CWE-1004 Sensitive Cookie Without 'HttpOnly' Flag

<b>Validation:</b>

This issue refers to SecurityConfiguration.java, line 42.
    
    return CookieCsrfTokenRepository.withHttpOnlyFalse();

<b>Explanation:</b> This setting is common for applications which have a JavaScript front-end and must be able to read the CSRF cookie (see AI logs). Since this application does not have a front end, this finding is valid.

## Manual findings
Manual findings were <b>not</b> detected by scanning tools.
_________
### Configuration

<b>CWE:</b> CWE-521 Weak Password Requirements (https://cwe.mitre.org/data/definitions/521.html)

<b>Validation:</b> DiscussionDtos.java, line 70-73, line 80-84

     public record UserRegistrationRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$") String username,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record UserProvisionRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$") String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull UserRole role) {

        }

<b>Explanation:</b> The application has a weak password policy. The existing policy in DiscussionDtos.java  enforces a minimum and maximum length, but this is its only protection. It does not require mixed character sets or meet standard minimum length requirements (16 characters - https://www.strong-password.com/guides/what-is-a-good-password-length/), especially considering it is one-factor authentication.

<b>CWE:</b> CWE-614 Sensitive Cookie in HTTPS Session Without 'Secure' Attribute (https://cwe.mitre.org/data/definitions/614.html)

<b>Validation:</b> <b>*FALSE POSITIVE*</b>

application.properties contains...

    server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:false}

<b>Explanation:</b> I kept this "finding" since I did note it to review later, so I consider this a manual false positive. Since this is local development without HTTPS, this setting is necessary (see AI logs).  

<b>CWE:</b> CWE-200: Exposure of Sensitive Information to an Unauthorized Actor (https://cwe.mitre.org/data/definitions/200.html)

<b>Validation:</b> In application.properties, the health/info actuator endpoints are exposed.

    management.endpoints.web.exposure.include=health,info

The info endpoint does not appear to be used in the actual application and may have been added by the AI as basic/template properties. The health endpoint is exposed publicly in SecurityConfiguration.java. 

    .requestMatchers("/actuator/health").permitAll()

<b>Explanation:</b> While this is not highly sensitive information, it could help with basic reconaissance efforts and expose information about the status/availability of the application unnecessarily.

<b>CWE:</b> CWE-400 Uncontrolled Resource Consumption (https://cwe.mitre.org/data/definitions/400.html)

<b>Validation:</b> I did not locate any dependencies or rate limit keywords during my code review that would suggest support for rate limiting.

<b>Explanation:</b> Without rate limits, I could theoretically freely rapidly send requests to the endpoints, especially when they are publicly accessible, in order to attempt to bring down the site for legitimate users.
__________
### Authorization

<b>CWE:</b> CWE-862 Missing Authorization (https://cwe.mitre.org/data/definitions/862.html)

<b>Validation:</b> Registration is self-service and publicly exposed in SecurityConfiguration.java, line 56.

    .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

GET requests under /api/v1/discussions/ have the .permitAll() rule, so authentication isn't required to view discussions about projects/labs/courses.

    .requestMatchers(HttpMethod.GET, "/api/v1/discussions/**").permitAll()

<b>Explanation:</b> Anyone can register and become an authenticated user. Though this public registration is limited to the student role, authenticated users have additional permissions, and in the context of a research/blog discussion board for students and faculty, self-registration is not an appropriate mechanism for account provisioning.

Additionally, making all project posts/comments publicly viewable is not necessarily advisable. There is a possibility that students and/or faculty will reveal sensitive information there. It is also necessary to protect the faculty and students' privacy in their work.

Finally, even authenticated users should not be able to see all courses, projects, and discussions. Faculty and students should be assigned to courses and projects, with their capabilities limited to retrieving and updating only data within those parameters.

<b>CWE</b> CWE-285 Improper Authorization (https://cwe.mitre.org/data/definitions/285.html)

<b>Validation:</b> Admins are permitted to edit the posts of other users in DiscussionService.java lines 304-308.

        private void ensureCanManage(String contentAuthor, String actor, boolean administrator) {
        if (!administrator && !contentAuthor.equalsIgnoreCase(actor)) {
            throw new AccessDeniedException("Only the author or an administrator can edit or delete this content.");
        }
    }

This was also confirmed via a Postman request documented in the conceptual journal audit log.

<b>Explanation:</b> While administrators should have elevated privileges, I do not believe they should necessarily be able to do *everything*. In the project specifications and subsequent AI build, administrators and faculty are granted the permission to moderate posts and comments, such as reporting and hiding them. I believe that the principle of least privilege is relevant here, and admins should not necessarily be permitted to edit any user's post. 

_________
### Input

<b>CWE:</b> CWE-204 Observable Response Discrepancy (https://cwe.mitre.org/data/definitions/204.html)

<b>Validation:</b> The registration page is publicly available in SecurityConfiguration.java.

    .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

The application produces an error when a username is already in use in UserAccountService.java lines 62-78.

    private UserResponse createUser(String username, String password, UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        validatePasswordBytes(password);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResourceConflictException("Username is already registered.");
        }
        return toResponse(persistUser(normalizedUsername, password, role));
    }

    private DiscussionUser persistUser(String username, String password, UserRole role) {
        try {
            return userRepository.saveAndFlush(new DiscussionUser(
                    username, passwordEncoder.encode(password), role));
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Username is already registered.");
        }
    }

<b>Explanation:</b> The registration page confirms if a username is already in use, which reveals a valid credential to a potential attacker. Since the registration page is publicly viewable, and anyone can register for an account, this becomes more of a security concern.

<b>CWE:</b> CWE-79 Improper Neutralization of Input During Web Page Generation - Stored/Persistent XSS (https://cwe.mitre.org/data/definitions/79.html)

<b>Validation:</b> The only management of post body user input is trimming white space in DiscussionService.java, line 233.

    post.setBody(request.body().trim())

The only management of comment body user input is trimming white space in DiscussionService.java, line 131 (create) and line 174 (update).

    comment.setMessage(request.message().trim());

This was also confirmed via a Postman request documented in the conceptual journal audit log.

<b>Explanation:</b> Post/comment body is expected to accept rich text, but there is no HTML sanitization prior to storing the user input in the database. Given the introduction of front-end rendering, this could increase the likelihood that a route neglects to enforce appropriate output encoding when displaying untrusted user input.