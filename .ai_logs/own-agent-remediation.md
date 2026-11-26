# Agent Remediation Prompt

## AI Deliverable Metadata Block
This document was written with...

AI contribution: 0%

Human contribution: 100%

## System / Role Prompt
Pretend you are a security engineer and your goal is to harden an existing service. This service is located in the secure-codebase folder. You must harden this service according to the guidance in this prompt while keeping the existing features and API functional. 

You may only refer to resources in the secure-codebase folder unless (1) you are running docker compose build on the repository, (2), you request permission, or (3) it is explicitly noted in this prompt. You may not use previous chat data.

Do not store any secrets in code or commits. If a secret is placed in a file, that file must be added to .gitignore. 

Do not delete or weaken existing tests. The only modification to existing tests that may be done is described in this prompt. 

When you are finished with your changes, you must run the tests and docker compose build.

## Security Constraints
### CWE and STRIDE threat mitigations
[CWE-1004] Sensitive tokens, including CSRF, should be HttpOnly and Secure.

[CWE-521] When a user sets a password, the password must be at least 16 characters, and must contain at least one uppercase letter, at least one lowercase letter, at least one digit, and at least one special character. Special characters should be restricted to non-harmful markup. The password must also have a maximum number of characters, such as 72.

[CWE-200] System endpoints, such as actuator endpoints, must require admin authentication to access.

[CWE-400] The application must be built to withstand resource consumption. Enforce rate limiting, including a minimum delay between expensive requests. If a user reaches 50 requests per minute, lock them out of the application for 10 seconds.

[CWE-862] Users must be added by administrators. The user will then need to set their password. Only a login endpoint is permitted to be public-facing, and everything else must require authentication. For authorization, users should be separated by Role-Based Access Control (RBAC) and all requests must verify the caller is authorized. See the Service-Context section for more information on those details.

[CWE-285] When an administrator takes privileged action on resources owned by other users, confirm the caller's identity, and if the identity is confirmed, create a visible tag or change that indicates the admin took action on it. See the Service-Context section for additional details.

[CWE-204] Do not reveal sensitive details through messages or timing discrepancies. For example, when logging in, a user should not be able to determine if the username is correct, whether by timing or by details in error messages.

[CWE-79] Sanitize input before sending it to the database to remove potentially harmful markup which may be used in a cross-site scripting attack. 

Repudiation - Log all activity with the username, action, resource ID, outcome, and timestamp. Do not include tokens or full request bodies.

### Input Validation
Make sure there is input validation on all write endpoints.

### Identity and Authorization

You must implement a decentralized, local authentication and authorization middleware layer using modern JSON Web Token (JWT) cryptographic APIs to enforce asymmetric key signature verification and deep row-level authorization logic. The minimum requirements are:
- RS256 or ES256 signature verification, with an algorithm allowlist (reject "none" and HS256)
- Validation of exp, iss, and aud claims
- Row-level authorization on your service's primary resource (see the Service-Context section)
- Application-layer encryption of the designated sensitive field (see the Service-Context section) with the key read from an environment variable and never committed

You must use the key-generation and mock token issuer tool provided in the mock-token-issuer folder. This folder contains a README about its use as well as a Spring Boot example in the examples subfolder.

**Identity.** The caller's identity is the JWT `sub` claim. Roles come from the JWT `roles` claim (a JSON array). The mock token issuer sets both, for example:

```
python mock_issuer.py issue --sub alice --roles faculty_instructor
```

**Roles used below.** `admin`, `faculty_instructor`, and `student` (any other authenticated student user). Domain 3 also uses `student_peer` for peer reviewers. You may add roles if your design needs them; document them in prompt-architecture.md.

**Ownership and membership data.** Store the owner's `sub` on each record when it is created (for example `owner_sub`, `requester_sub`, `reviewer_sub`, `author_sub`). Course and lab membership (which `sub` belongs to which course or lab, and who teaches it) comes from your service's local static mock data, not from token claims. Never take the owner from the request body.

**Row-level authorization.** Every read, update, and delete of a single record checks the caller against that record, not only against a role. Search and list endpoints return only the records the caller may read. For records the caller may not access, return either `403` or `404` consistently.

Minimum requirement (3) is the row-level check on the primary resource. The other role rules listed for each domain describe the expected authorization model around it.

**Application-layer encryption.**
- Encrypt the designated field in your service code before it is written to the database, and decrypt it only when returning it to a caller who is authorized to see it.
- Use authenticated encryption (AES-256-GCM or ChaCha20-Poly1305) with a unique nonce per value.
- Read the key from an environment variable (for example `FIELD_ENCRYPTION_KEY`). Never commit the key; commit only a placeholder in `.env.example`, keep `.env` in `.gitignore`, and pass the key to the container through Docker Compose (`env_file` or `environment` from your shell).
- An encrypted field cannot be searched or sorted in the database. Where the spec needs a search on that field, the note for that domain says how to handle it.

## Output and Verification Constraints

### Tests
Update all existing tests to support the new authentication/identity build. Then, create additional tests:
- A password set request with a password that does not meet the requirements (16 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character) is refused (CWE-521).
- A password set request with a password that does meet the requirements (16 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special character) is accepted (CWE-521).
- Demonstrate that only an admin user (JWT role) can reach sensitive endpoints, such as actuator endpoints (CWE-200).
- Demonstrate an anonymous user cannot access any endpoint other than the login page (CWE-862).
- Demonstrate that requests exceeding the rate limit are refused for 10 seconds (CWE-400).
- Demonstrate that an admin (JWT role) can successfully create a user (CWE-862).
- Demonstrate that non-admin (JWT role) users cannot create a user (CWE-862).
- Demonstrate that authorization rules only permit a user to access resources where they are assigned (CWE-862).
- Demonstrate that resources which are modified by administrators (JWT role), but are owned by another user, indicate admin modification (CWE-285).
- Demonstrate that HTTP response codes are consistent (403 vs 404) and that there are no timing differences when authenticating that would indicate correct vs incorrect credentials (CWE-204).
- Demonstrate that input is neutralized/sanitized (CWE-79).
- Demonstrate that user activity is logged successfully (STRIDE Repudiation).
- Demonstrate tests which confirm the expected test results documented in the mock token issuer tool "Negative tests" section as described in the README.
- [Instructor suggested] A request with a valid token for a different user (the mock issuer's `other_sub`) that tries to read, update, or delete another user's primary resource, and is refused.
- [Instructor suggested] A request from a lower-privilege role to an admin-only or faculty-only operation that is refused.
- [Instructor suggested] A check that the designated field is stored encrypted (read the raw database value in a test) and is returned decrypted to an authorized caller.

Run docker compose build and confirm the application builds properly.

Test/Production Generation Order: Explain if you wrote the code production-first, test-first, or together, and why.

## Service-Context Section

This application is Java / Spring Boot / H2 stack.

Student and faculty users must be assigned to courses/labs/projects in order to access them.

If an administrator edits another user's post or comment, confirm identity, and if identity is confirmed, clearly tag the post or comment as edited by an admin.

**Primary resource:** Root post (`/api/v1/discussions/posts/{post_id}`) and its comments.

**Row-level authorization:**
- Any authenticated caller may create posts and comments and read active content.
- Only the author (`author_sub`) or `admin` may update or delete a post or comment.
- Only `faculty_instructor` or `admin` may create an Academic Announcement post or change moderation status (`active`, `reported`, `hidden_by_moderator`), as the spec gives flagging to faculty roles.
- Content with status `hidden_by_moderator` is returned only to its author, `faculty_instructor`, and `admin`, including in timelines, searches, and the comment manifest download.

**Designated sensitive field:** Moderation note (the reason a moderator records when flagging or hiding a post or comment).

*Why:* A moderation note is a faculty judgment about a student's conduct and should be readable only by moderators.

*Notes:* If your PA2 data model has no field for this, add a moderation note (and the moderator's `sub`) to the status change on posts and comments. Post and comment bodies, titles, tags, and author handles stay unencrypted, because the spec requires keyword and author search over them.
