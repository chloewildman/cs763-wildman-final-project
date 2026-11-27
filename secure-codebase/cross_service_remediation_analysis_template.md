# PA3 CROSS-SERVICE REMEDIATION ANALYSIS (cross\_service\_remediation\_analysis.md)

Student Name: Chloe Wildman

Assigned Domain Module: Domain 4: Research Blog & Project Discussion Board

Core Technology Stack: Java / Spring Boot / H2 database

Keep it concise. Use one results table per repository, then the analysis and limitations note.

\================================================================================

AI DELIVERABLE METADATA BLOCK

\================================================================================

Conceptual AI Contribution to this document: 0%

Summary of design-aid prompts: I did not prompt AI to help with this .md file.

Chat share link(s) or appendix path in ./.ai\_logs/: [AI logs path PA3 release](../.ai_logs/project-3-release/)

\================================================================================

SECTION 1: REPOSITORIES

\================================================================================

| Field | Own repository | Peer repository |
| :---- | :---- | :---- |
| Repository URL (peer: your fork or copy) | https://github.com/chloewildman/cs763-wildman-final-project | https://github.com/chloewildman/cs763-wildman-ramirez-peer |
| Peer name (owner of original repo) | n/a | Daisy Ramirez |
| Domain module | Domain 4: Research Blog & Project Discussion Board | Domain 3: Peer Review and Evaluation Gateway |
| Stack | Java/Spring Boot/H2 | Python/FastAPI/PostgreSQL |
| Starting tag | project-2-release | v2.0.0 |
| pa3-measured tag | pa3-measured | pa3-measured |
| Iterations used (max 2) | 1 | 1 |
| Builds in Docker at pa3-measured | Yes | Yes |
| Scan tool(s) used | Semgrep, SonarQube, Trivy | Semgrep |

\================================================================================

SECTION 2: RESULTS TABLE, OWN REPOSITORY

\================================================================================

List every CWE from your sast-triage-report.md and your 3 STRIDE threats. Add rows as needed.

I added the "Short explanation" column here so I could explain a little about the fix as I reviewed the AI generated code.

| CWE or threat | Resolved in | New issues introduced | Short explanation | Fix tag |
| :---- | :---- | :---- | :---- | :---- |
| CWE-1004 Sensitive Cookie without "HttpOnly" flag | Iteration 1 | None | Cookies no longer used for auth, application.properties line 9 sets httponly. | pa3-measured |
| CWE-521: Weak Password Requirements | Iteration 1 | None | Requested password requirements set in UserAccountService.java. | pa3-measured |
| CWE-200 Exposure of Sensitive Information to Unauthorized Actor | Iteration 1 | None | Endpoint authorization in SecurityConfiguration.java has been updated so that only admins can reach /actuator/health. | pa3-measured |
| CWE-400 Uncontrolled Resource Consumption | Iteration 1 | None | Requested rate limit parameters were established in RequestRateLimiter.java and RequestRateLimitFilter.java, which is then added as a Spring Security filter in SecurityConfiguration.java. The windows appear to be programmed to reset and cleaned properly. | pa3-measured |
| CWE-862 Missing Authorization | Iteration 1 (partially resolved) | None | Only the login endpoint is .permitAll() in SecurityConfiguration.java, and users must be provisioned by admins. Non-admin users are assigned to projects and courses and cannot access resources they are not assigned to. This is done throught he service layer, particularly canAccessScope() in DiscussionService.java. This relies on hard-coded membership in LocalMembershipRegistry.java rather than admin-assignment, which for the purposes of this assignment should be fine. CAN FACULTY SEE HIDDEN MODERATION FOR COURSES THEY ARE NOT ASSIGNED TO? | pa3-measured |
| CWE-285 Improper Authorization | Iteration 1 | None | DiscussionService.java has been updated to use markAdminAction() methods, which check the caller's identity and role to determine if the caller is an admin and is not the owner of the post/comment, and sets admin modification indicators. | pa3-measured |
| CWE-204 Observable Response Discrepancy | Iteration 1 | None | Users can only be provisioned by admins now, so username enumeration from the registration page has been remediated. | pa3-measured |
| CWE-79 Improper Neutralization of Input During Web Page Generation | Iteration 1 | No CWEs, see explanation | Remediation went too far and completely sanitized all HTML markup from input, but specs require that post and comment bodies can use rich text. | pa3-measured (initial), DOCUMENT FIX |
| T1: Repudiation (Service/business layer) | Iteration 1 (partially resolved) | No CWEs, see explanation | Audit logging is introduced through AuditLoggingFilter.java and added as a Spring Security filter in SecurityConfiguration.java; however, the log doesn't get stored somewhere that would be useful for non-repudiation later. | pa3-measured (initial), DOCUMENT FIX |
| T2: Denial of service (Spring Security): CWE-400 | See CWE-400 | See CWE-400 | See CWE-400 | See CWE-400 |
| T3: Tampering (Service/business layer): CWE-79 | See CWE-79 | See CWE-79 | See CWE-79 | See CWE-79 |

\================================================================================

SECTION 3: RESULTS TABLE, PEER REPOSITORY

\================================================================================

List the CWEs you triaged from your own Semgrep run on the peer's PA2 tag (Semgrep is sufficient) and the threats you targeted through the service-context section. Do not use the peer's security-analysis.md. Add rows as needed. If you do the optional full peer remediation for extra credit, log it separately, not in this table.

NO SEMGREP FINDINGS FOR PEER REPO. COMPARING CWEs FROM PROMPTING BELOW.

I added the "Short explanation" column here so I could explain a little about the fix as I reviewed the AI generated code.

| CWE or threat | Resolved in | New issues introduced | Short explanation | Fix tag |
| :---- | :---- | :---- | :---- | :---- |
| CWE-1004 Sensitive Cookie without "HttpOnly" flag | [] | [] | [] | [] |
| CWE-521: Weak Password Requirements | [] | [] | [] | [] |
| CWE-200 Exposure of Sensitive Information to Unauthorized Actor | [] | [] | [] | [] |
| CWE-400 Uncontrolled Resource Consumption | [] | [] | [] | [] |
| CWE-862 Missing Authorization | [] | [] | [] | [] |
| CWE-285 Improper Authorization | [] | [] | [] | [] |
| CWE-204 Observable Response Discrepancy | [] | [] | [] | [] |
| CWE-79 Improper Neutralization of Input During Web Page Generation | [] | [] | [] | [] |
| T1: Repudiation (Service/business layer) | [] | \[None, or CWE / description\] | [] | \[pa3-measured / later tag / n/a\] |
| T2: Denial of service (Spring Security): CWE-400 | See CWE-400 | See CWE-400 | See CWE-400 | See CWE-400 |
| T3: Tampering (Service/business layer): CWE-79 | See CWE-79 | See CWE-79 | See CWE-79 | See CWE-79 |

\================================================================================

SECTION 4: COMPARATIVE ANALYSIS

\================================================================================

4.1 Effectiveness: Own vs. Peer

Compare resolution rates and the kinds of issues that were or were not resolved in each repository. Explain the differences.

\[STUDENT ANALYSIS TEXT HERE\]

4.2 Agent Behavior

How did the agent behave in an unfamiliar service and stack? (e.g., ignored constraints, broke the build, hallucinated framework APIs, introduced new weaknesses)

\[STUDENT ANALYSIS TEXT HERE\]

4.3 Prompt Architecture Lessons

What did the service-context section have to carry for the peer repository? What would you change in the prompt architecture so it transfers better across services?

\[STUDENT ANALYSIS TEXT HERE\]

\================================================================================

SECTION 5: LIMITATIONS NOTE ON PEER TRIAGE QUALITY

\================================================================================

Describe how confident you are in the peer results. (e.g., Semgrep-only coverage, unfamiliar domain and stack, no line-by-line review, possible missed false positives or false negatives, time spent)

\[STUDENT LIMITATIONS TEXT HERE\]