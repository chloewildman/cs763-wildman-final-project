# Security Analysis

## Security requirements
- Authorization controls
    - Registration should be controlled by admins only, not publicly.
    - Users should be assigned to courses and projects, and should only be able to access/post to those endpoints.
    - Mass downloads such as the comment manifest should be restricted to faculty and admins.

## Abuse cases
- AC1: As an attacker, I want to be able to access high-privilege capabilities so that add other high-privilege users, edit/delete other user's posts, and access the moderation endpoint.
- AC2: As an attacker, I want to be able to access credential information so that I can begin finding valid credentials of users with high-privilege capabilities.
- AC3: As an attacker, I want to be able to serve malicious content so that I can deface the site or expand the attack to legitimate users.
- AC4: As an attacker, I want to be able to remotely take down the site so that legitimate users can no longer access it.
- AC5: As an attacker, I want to be able to craft malicious inputs so that I can get the application to return valuable information from the database.
- AC6: As an attacker, I want to be able to gain information about the system so that I can better plan my attacks.
- AC7: As an attacker, I want to be able to find sensitive information about users or the organization so that I can better plan my attacks.

## Attack trees
### AC1

### AC3

### AC4

## Attack surface mapping


## STRIDE threat modeling (DFD)

## Proposed mitigations
### CWE-1004 Sensitive Cookie Without 'HttpOnly' Flag
Remove the .withHttpOnlyFalse() configuration.

Relates to: AC3

### CWE-521 Weak Password Requirements (https://cwe.mitre.org/data/definitions/521.html)
Enforce stronger password requirements. For the purposes of this application, replace regex and error message to reflect at least 16 characters and at least one uppercase, lowercase, digit, and special character ().

Regex: `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9])[^\r\n]{16,72}$` ([log-link])

Relates to: AC2

### CWE-614 Sensitive Cookie in HTTPS Session Without 'Secure' Attribute (https://cwe.mitre.org/data/definitions/614.html)
N/A, false positive

### CWE-200: Exposure of Sensitive Information to an Unauthorized Actor (https://cwe.mitre.org/data/definitions/200.html)
Require authentication as an admin to reach the health endpoint.

Relates to: AC6

### CWE-400 Uncontrolled Resource Consumption (https://cwe.mitre.org/data/definitions/400.html)
Implement rate limiting. For the purposes of this assignment, a simple custom filter-based rate limiter for something like 50 requests per minute (or less for demo purposes) seems appropriate (https://www.geeksforgeeks.org/advance-java/implementing-rate-limiting-in-a-spring-boot-application/).

Relates to: AC4

### CWE-862 Missing Authorization (https://cwe.mitre.org/data/definitions/862.html)
Refactor code so that users must be added by administrators. Make only the login endpoint public-facing, and everything else must require authentication. For authorization, users should continue being separated by Role-Based Access Control (RBAC), but student/faculty users should also be explicitly assigned to courses and projects, and should only be permitted to access those endpoints.

Relates to: AC7

### CWE-285 Improper Authorization (https://cwe.mitre.org/data/definitions/285.html)
While administrators should be granted high-privilege permissions, not all permissions are necessary to fulfill the role. An admin should not be permitted to change the contents of another user's existing post, or delete existing posts, as long as there is moderation functionality.

Relates to: AC1, AC3

### CWE-204 Observable Response Discrepancy (https://cwe.mitre.org/data/definitions/204.html)
This vulnerability will be addressed by the proposed mitigation for CWE-862. As long as users can only be added by administrators, only administrators would receive confirmation of existing accounts, which is part of their expected high-privilege capabilities.

Relates to: AC2

### CWE-79 Improper Neutralization of Input During Web Page Generation - Stored/Persistent XSS (https://cwe.mitre.org/data/definitions/79.html)
Sanitize post/comment body input using Java's HTML Sanitizer (https://owasp.org/projects/java-html-sanitizer).

Relates to: AC3