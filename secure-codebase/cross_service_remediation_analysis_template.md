# PA3 CROSS-SERVICE REMEDIATION ANALYSIS (cross\_service\_remediation\_analysis.md)

Student Name: Chloe Wildman

Assigned Domain Module: Domain 4: Research Blog & Project Discussion Board

Core Technology Stack: Java / Spring Boot / H2 database

Keep it concise. Use one results table per repository, then the analysis and limitations note.

\================================================================================

AI DELIVERABLE METADATA BLOCK

\================================================================================

Conceptual AI Contribution to this document: 0%

Summary of design-aid prompts: \[One or two sentences\] 

Chat share link(s) or appendix path in ./.ai\_logs/: \[Insert link or path\]

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
| Iterations used (max 2\) | \[ \] | \[ \] |
| Builds in Docker at pa3-measured | \[Yes / No, broken by agent\] | \[Yes / No, broken by agent\] |
| Scan tool(s) used | Semgrep, SonarQube, Trivy | Semgrep |

\================================================================================

SECTION 2: RESULTS TABLE, OWN REPOSITORY

\================================================================================

List every CWE from your sast-triage-report.md and your 3 STRIDE threats. Add rows as needed.

| CWE or threat | Resolved in | New issues introduced | Fix tag |
| :---- | :---- | :---- | :---- |
| CWE-1004 Sensitive Cookie without "HttpOnly" flag | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-521: Weak Password Requirements | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-200 Exposure of Sensitive Information to Unauthorized Actor | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-400 Uncontrolled Resource Consumption | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-862 Missing Authorization | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-285 Improper Authorization | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-204 Observable Response Discrepancy | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| CWE-79 Improper Neutralization of Input During Web Page Generation | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| T1: Repudiation (Service/business layer) | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| T2: Denial of service (Spring Security): CWE-400 | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |
| T3: Tampering (Service/business layer): CWE-79 | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / later tag / n/a\] |

\================================================================================

SECTION 3: RESULTS TABLE, PEER REPOSITORY

\================================================================================

List the CWEs you triaged from your own Semgrep run on the peer's PA2 tag (Semgrep is sufficient) and the threats you targeted through the service-context section. Do not use the peer's security-analysis.md. Add rows as needed. If you do the optional full peer remediation for extra credit, log it separately, not in this table.

| CWE or threat | Resolved in | New issues introduced | Fix tag |
| :---- | :---- | :---- | :---- |
| \[CWE-xxx: short name\] | \[Iteration 1 / Iteration 2 / Not resolved\] | \[None, or CWE / description\] | \[pa3-measured / n/a\] |

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