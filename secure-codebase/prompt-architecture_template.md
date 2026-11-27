# PA3 PROMPT ARCHITECTURE (prompt-architecture.md)

Student Name: Chloe Wildman

Assigned Domain Module: Domain 4: Research Blog & Project Discussion Board

Core Technology Stack: Java / Spring Boot / H2 database

Own Repository URL: https://github.com/chloewildman/cs763-wildman-final-project

Peer Repository URL (your fork or copy, where pa3-measured is tagged): https://github.com/chloewildman/cs763-wildman-ramirez-peer

Peer Domain Module and Stack: Domain 3: Peer Review and Evaluation Gateway, Python / FastAPI / PostgreSQL

Use these headings and metadata fields as given. The prompt content in Section 3 is your own.

================================================================================

AI DELIVERABLE METADATA BLOCK

================================================================================

Conceptual AI Contribution to this document: 0%

Summary of design-aid prompts: I did not prompt AI to help with this .md file.

Chat share link(s) or appendix path in ./.ai_logs/: [AI logs path PA3 release](../.ai_logs/project-3-release/)

================================================================================

SECTION 1: ENGINE AND RUN METADATA

================================================================================

1.1 Engine and Model

| Field | Value |
|---|---|
| Agent tool and version | VSCode Github Copilot |
| Engine / provider | OpenAI |
| Model string | GPT-6 Luna |
| Operational mode | Autopilot |
| Hyperparameters changed from PA1 Agent_environment.md | None |

1.2 Test/Production Generation Order

Order used: Production-first in both own and peer repo.

Brief reason: 

In own repo, the agent provided the reasoning, "The security model and production implementation were established first because moving from sessions to issuer-validated JWTs changed the authentication boundary, role mapping, and account lifecycle." (see AGENTMADE-prompt-architecture.md)

In peer repo, the agent provided the reasoning, "The authentication and persistence controls span shared middleware and routes, so I added integrated tests after wiring them in, while retaining the existing functional assertions." (see AI logs)

1.3 Iteration Count (measured run, maximum 2 per repository)

| Repository | Starting tag | Iterations used | Ending tag | Build result at pa3-measured |
|---|---|---|---|---|
| Own | project-2-release | 1 | pa3-measured | Builds |
| Peer | v2.0.0 | 1 | pa3-measured | Builds |

================================================================================

SECTION 2: REMEDIATION SCOPE

================================================================================

2.1 Confirmed CWEs (all of them, from your sast-triage-report.md; add rows as needed)

| # | CWE ID | Location (file / endpoint) | Short description |
|---|---|---|---|
| 1 | [CWE-1004] | SecurityConfiguration.java, line 52 | A sensitive cookie uses withHttpOnlyFalse(), so it does not have the HttpOnly flag, meaning that JavaScript can access the cookie and be prone to XSS. |
| 2 | [CWE-521] | DiscussionDtos.java, line 80-83, line 90-94 | The application has a weak password policy with only min/max length. |
| 3 | [CWE-200] | application.properties line 11, SecurityConfiguration.java line 64 | The health actuator endpoint is public, but should require authentication. |
| 4 | [CWE-400] | N/A | The application does not support any form of rate limiting, so an attacker could possibly take down the site due to resource consumption by flooding it with requests, particularly at the public endpoints. |
| 5 | [CWE-862] | Security Configuration.java, line 66 and 73 | Anyone can register and become an authenticated user. All project posts/comments are publicly viewable, Authenticated users should not be able to see all courses, projects, and discussions. |
| 6 | [CWE-285] | DiscussionService.java lines 304-308 | Administrators are permitted to edit other user's posts despite having moderation privileges. |
| 7 | [CWE-204] | SecurityConfiguration.java line 66, UserAccountService.java lines 72-88 |  The registration page confirms if a username is already in use, which reveals a valid credential to a potential attacker. Since the registration page is publicly viewable, and anyone can register for an account, this becomes more of a security concern. |
| 8 | [CWE-79] | DiscussionService.java, lines 243, 141, 184 | Post/comment body is expected to accept rich text, but there is no HTML sanitization prior to storing the user input in the database. |

2.2 STRIDE Threats (exactly 3, from your security-analysis.md)

| # | STRIDE category | DFD component | One-line risk rationale |
|---|---|---|---|
| T1 | Repudiation | Service/business layer | A user edits their post with harmful information and denies they did it. |
| T2 | Denial of service (CWE-400) | Spring Security | A user floods an endpoint with several and/or expensive requests, consuming all available resources and compromising availability. |
| T3 | Tampering (CWE-79) | Service/business layer (where white space is currently trimmed) | A malicious actor embeds harmful HTML markup in stored post/comment text and could execute in a victim's browser.  |

================================================================================

SECTION 3: SECURE-BY-DESIGN PROMPT ARCHITECTURE

================================================================================

Record the prompts exactly as given to the agent. Sections 3.1 to 3.3 are applied unchanged to both repositories. Section 3.4 is the one labeled service-context section, and it is the only part that may differ between repositories. Section 3.5 records the scan output added in iteration 2, if used.

3.1 System / Role Prompt

Lines 10-20 of [own-agent-remediation.md](../.ai_logs/own-agent-remediation.md) and [peer-agent-remediation.md](../.ai_logs/peer-agent-remediation.md)

3.2 Security Constraints

Show where your prompt covers each minimum requirement.

| Requirement | Prompt section / line that enforces it |
|---|---|
| (1) RS256 or ES256 verification with algorithm allowlist (reject "none" and HS256) | Line 47 |
| (2) exp, iss, and aud claim validation | Line 48 |
| (3) Row-level authorization on the primary resource | Line 49 & 66; Line 107 (peer) 111 (own) |
| (4) Application-layer encryption of the sensitive field, key from environment variable, never committed | Line 50 & 70; Line 115 (peer) 117 (own) |
| (5) Input validation on all write endpoints | Line 42 |
| Remediation of each CWE in 2.1 | Line 23-38 |
| Mitigation of each threat in 2.2 | Line 39; Line 29; Line 37 |

Lines 21-75 of [own-agent-remediation.md](../.ai_logs/own-agent-remediation.md) and [peer-agent-remediation.md](../.ai_logs/peer-agent-remediation.md)

3.3 Output and Verification Constraints

(e.g., required tests, Docker Compose build, PA2 integration tests updated for authentication, file header and commit tag rules)

Lines 76-100 of [own-agent-remediation.md](../.ai_logs/own-agent-remediation.md) and [peer-agent-remediation.md](../.ai_logs/peer-agent-remediation.md)

3.4 Service-Context Section (the only labeled section that changes per repository)

Include that repository's domain, stack, primary resource, and designated sensitive field (see PA3_domain_security_definitions.md in the Google Drive folder).

3.4.a Own repository

Lines 76-122 of [own-agent-remediation.md](../.ai_logs/own-agent-remediation.md)

3.4.b Peer repository

Lines 76-120 of [peer-agent-remediation.md](../.ai_logs/peer-agent-remediation.md)

3.5 Iteration 2 Input (if used)

Iteration 2 may add only the scan output from iteration 1. State what was added.

| Repository | Scan output added (tool, file or excerpt location) |
|---|---|
| Own | Not used |
| Peer | Not used |

================================================================================

SECTION 4: MEASURED RUN LOG

================================================================================

One row per iteration per repository.

| Date | Repository | Iteration | Agent stop reason | Scan tool(s) | Findings before / after | Notes |
|---|---|---|---|---|---|---|
| [2026-11-26] | Own | 1 | Agent completed code updates and docker compose build. | Semgrep, SonarQube, Trivy | 0 Semgrep findings; 1 Trivy finding (outdated Trivy version); 2 SonarQube security issues (disabled CSRF protection, PASSWORD detected in expression) | See notes beneath table |
| [2026-11-27] | Peer | 1 | Agent completed code updates and docker compose build. | Semgrep | 0 Semgrep findings | No notes |

Own repo scanner notes
- Outdated Trivy version: I reviewed the Github repo, and latest published version for Actions still aligns with mine (https://github.com/aquasecurity/trivy-action). Considering a false positive.
- Disabled CSRF protection: This warranted the most investigation. I reviewed an online resource (https://www.baeldung.com/csrf-stateless-rest-api) and asked the AI agent to explain this change (see AI logs). The AI agent designed the service as a stateless API, which validates the JWTs through the Authorization: Bearer header, not through a cookie. This design reduces exposure to traditional CSRF attacks because browsers do not automatically attach Authorization headers to cross-site requests. However, the JWT would need to be stored somewhere in the client, which opens up a larger attack surface for XSS, so the application must be well-protected in that regard.
- PASSWORD detected in expression: False positive. It is referring to the "PASSWORD_PATTERN" variable name in DiscussionDtos.java, which only stores the password regex.

================================================================================

SECTION 5: POST-MEASURED HARDENING (OWN REPOSITORY ONLY)

================================================================================

Summarize changes made after the pa3-measured tag and how they were made (AI, HUMAN, or HYBRID commits). 

| Commit tag | Change | Reason |
|---|---|---|
| [HUMAN: / HYBRID: / AI:] | Fix post/comment body sanitization | CWE-79 remediation sanitized ALL HTML markup, but specs require that post and comment bodies can be rich text. |
| [HUMAN: / HYBRID: / AI:] | Fix faculty moderator access issues | The introduction of the sensitive field and new moderator rules seemed to influence the AI to allow faculty moderators to view hidden content even of courses/projects they are not assigned to. |
| [HUMAN: / HYBRID: / AI:] | Fix lack of log storage | While audit logs are generated, they are not stored in a way that would be useful for non-repudiation. |