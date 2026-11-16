# Security Analysis

## AI Deliverable Metadata Block
This document was written with...
AI contribution: 0%
Human contribution: 100%

## Security requirements
- Passwords must contain at least 16 characters with an uppercase letter, lowercase letter, number and special character.
- All endpoints other than the login endpoint must require authentication, including GET requests.
- Accounts must be delineated into role-based access control (RBAC) through admin, faculty, and student roles.
- All account provisioning must only be controlled by admins.
- Actuator endpoints must only be accessible by admins.
- Faculty and student users must be assigned to courses and projects, and must only be able to access/post to those assigned endpoints.
- Mass downloads such as the comment manifest must be restricted to faculty and admin roles.
- All user input that may contain HTML markup (rich text) must be sanitized of malicious tags at the input stage.
- Only the author of a post may be permitted to modify it. Even administrators may not modify other posts while moderation privileges are present.
- Users must be restricted to no more than 50 API requests per minute.
- User API activity must be logged with timestamps and username.

## Abuse cases
- AC1: As an attacker, I want to be able to access high-privilege capabilities so that add other high-privilege users, edit/delete other user's posts, and access the moderation endpoint.
- AC2: As an attacker, I want to be able to access credential information so that I can begin finding valid credentials of users with high-privilege capabilities.
- AC3: As an attacker, I want to be able to serve malicious content so that I can deface the site or expand the attack to legitimate users.
- AC4: As an attacker, I want to be able to disrupt availability of the application so that legitimate users can no longer use it.
- AC5: As an attacker, I want to be able to craft malicious inputs so that I can get the application to return valuable information from the database.
- AC6: As an attacker, I want to be able to gain information about the system so that I can better plan my attacks.
- AC7: As an attacker, I want to be able to find sensitive information about users or the organization so that I can better plan my attacks.

## Attack trees
I tried to focus the contents of these attack trees on potentially exploitable weaknesses in the application, so there is not any focus in these trees on social engineering/physical security.
### AC1
See [AC1 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC1.png)

Or: ![AC1 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC1.png)

### AC3
See [AC3 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC3.png)

Or: ![AC3 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC3.png)

### AC4
See [AC4 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC4.png)

Or: ![AC4 Attack Tree in docs](functional-codebase/docs/Attack%20Trees%20PA2%20-%20AC4.png)

## Attack surface mapping
I've placed a ! in the flag column of the highest risk surfaces.
| Endpoint | Method | Auth Required | User Input | Risks | Flag |
|----------|--------|---------------|------------|-------|------|
| /register | POST | No | username, password | Username enumeration (CWE-204), missing authorization (CWE-862), no rate limit (CWE-400) | ! |
| /admin/users | POST | Yes, admin only | username, password, role | Risks would relate to a compromised admin/facult account due to username enumeration (CWE-204) and weak password policy (CWE-521), but still high risk due to impact of creating other admin users | ! | 
| /login | POST | No | username, password | Weak password policy (CWE-521), no rate limit (CWE-400) | ! |
| /me | GET | Yes | None | None | |
| /logout | POST | Yes | None | None | |
| /acuator/health | GET | No | None | Exposure of sensitive information (CWE-200) | |
| /discussions/posts | GET | No | None | Missing authorization, no authentication required (CWE-862), no rate limit (CWE-400) | | 
| /discussions/posts | POST | Yes | title, body (rich text), category, tags, projectId, courseId, labId | Cross-site scripting (CWE-79) or other malicious content, no rate limit (CWE-400) | ! | 
| /discussions/posts/{id} | GET | No | None | Missing authorization, no authentication required (CWE-862), no rate limit (CWE-400) | |
| /discussions/posts/{id} | PUT | Yes, author or admin | title, body (rich text), category, tags, projectId, courseId, labId | Cross-site scripting (CWE-79) or other malicious content, Improper authorization (CWE-285), no rate limit (CWE-400) | ! |
| /discussions/posts/{id} | DELETE | Yes, author or admin | None | Improper authorization (CWE-285), no rate limit (CWE-400) | |
| /discussions/scopes/{scope_type}/{scope_id}/timeline | GET | No | None | Missing authorization, no authentication required (CWE-862), no rate limit (CWE-400) | |
| /discussions/posts/{post_id}/comments | GET | No | None | Missing authorization, no authentication required (CWE-862), no rate limit (CWE-400) | | 
| /discussions/posts/{post_id}/comments | POST | Yes | body (rich text) | Cross-site scripting (CWE-79) or other malicious content, no rate limit (CWE-400) | ! | 
| /discussions/posts/{post_id}/comments/{comment_id} | GET | No | None | Missing authorization, no authentication required (CWE-862), no rate limit (CWE-400) | | 
| /discussions/posts/{post_id}/comments/{comment_id} | PUT | Yes, author or admin | body (rich text) | Cross-site scripting (CWE-79) or other malicious content, Improper authorization (CWE-285), no rate limit (CWE-400) | ! | 
| /discussions/posts/{post_id}/comments/{comment_id} | DELETE | Yes, author or admin | None | Improper authorization (CWE-285), no rate limit (CWE-400) | | 
| /discussions/posts/{post_id}/moderation | PATCH | Yes, faculty or admin | Moderation status | Minimal inherent risks at this endpoint. Risks would relate to a compromised admin/facult account due to username enumeration (CWE-204) and weak password policy (CWE-521) | | 
| /discussions/posts/{post_id}/comments/{commentId}/moderation | PATCH | Yes, faculty or admin | Moderation status | Minimal inherent risks at this endpoint. Risks would relate to a compromised admin/facult account due to username enumeration (CWE-204) and weak password policy (CWE-521) | | 

## STRIDE threat modeling (DFD)
Diagram is based on *current* application, not desired application with all stated requirements.
See [DFD](functional-codebase/docs/DFD.png)

Or: ![DFD](functional-codebase/docs/DFD.png)

STRIDE matrix below indicates which of the STRIDE categories could apply to each component from the DFD. The corresponding IDs are included in the mitigation section to help map abuse cases, attack trees, and DFD/STRIDE components to actual mitigations.

| ID | Components | Spoofing | Tampering | Repudiation | Information disclosure | Denial of service | Elevation of privilege |
|----|------------|----------|-----------|-------------|------------------------|-------------------|------------------------|
|S1| Auth admin | X | | X | X | X | X |
|S2| Auth faculty | X | | X | X | X | X |
|S3| Auth 'student' | X | | X | X | X | X |
|S4| Anonymous user | X | | | X | X | |
|S5| Spring Security | X | X | X | X | X | X |
|S6| Controllers/DTOs | X | X | X | X | X | X |
|S7| Service/business logic | | X | X | X | X | X |
|S8| Repository layer | | X | X | X | | X |
|S9| Database | | X | X | X | | X |

## Proposed mitigations
### CWE-1004 Sensitive Cookie Without 'HttpOnly' Flag
Remove the .withHttpOnlyFalse() configuration.

Relates to: AC3, S5 Information Disclosure & Spoofing

### CWE-521 Weak Password Requirements (https://cwe.mitre.org/data/definitions/521.html)
Enforce stronger password requirements. For the purposes of this application, replace regex and error message to reflect at least 16 characters and at least one uppercase, lowercase, digit, and special character ().

Regex: `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9])[^\r\n]{16,72}$` (see AI logs)

Relates to: AC2, S1/S2/S3 -> S6 Spoofing & Elevation of Privilege

### CWE-614 Sensitive Cookie in HTTPS Session Without 'Secure' Attribute (https://cwe.mitre.org/data/definitions/614.html)
N/A, false positive

### CWE-200: Exposure of Sensitive Information to an Unauthorized Actor (https://cwe.mitre.org/data/definitions/200.html)
Require authentication as an admin to reach the health endpoint.

Relates to: AC6, S5 Information Disclosure

### CWE-400 Uncontrolled Resource Consumption (https://cwe.mitre.org/data/definitions/400.html)
Implement rate limiting. For the purposes of this assignment, a simple custom filter-based rate limiter for something like 50 requests per minute (or less for demo purposes) seems appropriate (https://www.geeksforgeeks.org/advance-java/implementing-rate-limiting-in-a-spring-boot-application/).

Relates to: AC4, S1/S2/S3 -> S4/S5 Denial of Service

### CWE-862 Missing Authorization (https://cwe.mitre.org/data/definitions/862.html)
Refactor code so that users must be added by administrators. Make only the login endpoint public-facing, and everything else must require authentication. For authorization, users should continue being separated by Role-Based Access Control (RBAC), but student/faculty users should also be explicitly assigned to courses and projects, and should only be permitted to access those endpoints.

Relates to: AC7, S4 -> S5/S6/S7 Spoofing, Tampering, Information Disclosure, Elevation of Privilege, S2/S3 -> S5/S6/S7 Information Disclosure

### CWE-285 Improper Authorization (https://cwe.mitre.org/data/definitions/285.html)
While administrators should be granted high-privilege permissions, not all permissions are necessary to fulfill the role. An admin should not be permitted to change the contents of another user's existing post, or delete existing posts, as long as there is moderation functionality.

Relates to: AC1, AC3, S1 -> S5/S6/S7 Tampering

### CWE-204 Observable Response Discrepancy (https://cwe.mitre.org/data/definitions/204.html)
This vulnerability will be addressed by the proposed mitigation for CWE-862. As long as users can only be added by administrators, only administrators would receive confirmation of existing accounts, which is part of their expected high-privilege capabilities.

Relates to: AC2, S4 -> S5/S7 Information Disclosure & Spoofing

### CWE-79 Improper Neutralization of Input During Web Page Generation - Stored/Persistent XSS (https://cwe.mitre.org/data/definitions/79.html)
Sanitize post/comment body input using Java's HTML Sanitizer (https://owasp.org/projects/java-html-sanitizer).

Relates to: AC3, S1/S2/S3 -> S7 Tampering & Elevation of Privilege