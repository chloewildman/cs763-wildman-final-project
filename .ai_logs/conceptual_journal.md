# MASTER CONCEPTUAL JOURNAL & AI TELEMETRY LOG

* **Student Name:** Chloe Wildman
* **Assigned Domain Module:** Domain 4: The Research Blog & Project Discussion Board
* **Core Technology Stack:** Java/Spring Boot

## AI Deliverable Metadata Block
This document was written with...
AI contribution: 0%
Human contribution: 100%

\================================================================================

## SECTION 1: SYSTEM ENVIRONMENT & TOOL SPECIFICATION

\================================================================================

### 1.1 AI Tool & Engine Deployments

*Identify all autonomous developer agents, conversational LLMs, inline assistants, or documentation tools utilized during this project milestone.*

| Tool Context | Framework / Platform Name & Version | Underlying LLM Brain Model String | Operational Setting / Access Mode |
| :---- | :---- | :---- | :---- |
| **Code Generation** | GitHub Copilot (VS Code Extension) 1.24.0 | GPT-6 Luna | VSCode Extension - In-app chat |


### 1.2 Verification Telemetry Access Checklist

- [X] Primary Auto-Extracted Session Logs / Appendices Saved to `/.ai_logs/` directory  
- [N/A] External Chat Cloud Share Link 1 (If applicable): \[Insert URL Here\]  
- [N/A] External Chat Cloud Share Link 2 (If applicable): \[Insert URL Here\]

\================================================================================

## SECTION 2: COGNITIVE REFLECTION — CODE GENERATION & COMPREHENSION

\================================================================================

### 2.1 The Architectural Flow (How You Understand the Code)

*Provide a rigorous, detailed architectural breakdown explaining the data path your AI tool generated for your core routes (e.g., /api/v1/assets/pools/allocate). Describe how an incoming HTTP request payload is captured by your framework, parsed by your middleware, processed via database queries, and safely closed or rolled back inside your transaction engine. State what design patterns (Repository, MVC, Singleton, etc.) the AI decided to adopt.*

In general, the HTTP request first hits the security filters set through Spring Security, such as session management and authentication, authorization, and CSRF tokens (see SecurityConfiguration.java). Initial login is managed by AuthController.java, though the AI-generated code did not require authentication to all routes. For example, registration and login do not require authentication. Neither do GET requests to paths under /api/v1/discussions/ (more on that later).

After that, the URL and HTTP method gets matched by Spring MVC to the Controller layer (found in the api folder). When the request has a body to parse, it gets mapped into a DTO (found in DiscussionDtos.java).

The controller then calls on the service layer (in the service folder), which manages the business logic, such as whether or not a comment exists before updating it. The service layer then calls on the repository layer for database operations (see repository folder). Key services in the app (DiscussionService.java) are using the Transactional annotation, which brings in Spring's transaction management so that if a process completes normally, the change is committed, but unchecked exceptions or errors result in a safe roll back by default (https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html). Additionally, the repository layers extend JPA Repository. JPA allows developers to create queries from method names which are safely translated into a SQL query at runtime (https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html). Queries are not concatenated in this application. The database is H2.

The response DTO and HTTP response is then returned to the client.

As far as design patterns, the AI followed what I believe to be a standard Spring Boot REST/MVC layered structure with repository and service layers. 

Of note, the AI generated code also included an endpoint that was not requested to allow PATCH requests to moderation endpoints by authenticated admins and faculty (aside from endpoints related to authentication, which were intentionally not provided in the specs). This appears to have been done to meet the requirement, "Provide an administrative moderation workflow to enforce community standards across text inputs. The service must support state transitions (active, reported, hidden_by_moderator) applied independently to either root posts or child comments, allowing faculty roles to flag inputs and trigger cascading visibility adjustments across the board."


### 2.2 Defensive Edge-Case Evaluation (Line-by-Line Code Review)

*Perform a line-by-line human audit of the generated source code. Identify where the AI tool completely failed to account for edge cases, unusual boundaries, or unexpected system properties prior to running your pipeline scanners. (e.g., What happens if an integer balance value scales to a negative integer? How does the routing loop handle an invalid JSON payload sequence?)*

The most glaring security boundary that AI did not account for relates to authentication, authorization, and information disclosure. While sending Postman requests and completing code review, I realized that the authentication configuration had a weak password policy of just 12 characters and no diversity of character sets. In terms of authorization, posts/comments were all publicly visible even to anonymous users, and only creating/modifying posts and comments, or accessing high-privilege permissions like moderation, required authentication. Moreover, registration itself was on a public page. Any anonymous user could register and become a "student", allowing them to make posts and comments freely. Since that registration page is public, and produces a message about a username already in use (located during code review), this allows for username enumeration. 

While submitting postman requests, I also noted that submitting a post body that contains script tags is not sanitized before being returned. This allows harmful tags to be stored in the database, and then potentially be returned to users if not handled properly when rendering.

Config: Check application.properties for SESSION_COOKIE_SECURE=false, meaning session cookie might not always be encrypted through HTTPS. Health/info actuator endpoints are exposed and could help with basic reconaissance.

Finally, in reflecting on what other vulnerabilties that I've seen previously could apply here, I considered denial of service attacks. I reviewed the code for any implementation of API rate limiting, and did not note any.

### 2.3 The AI Agent's "Security Sins" (Evaluating Code Deficiencies)

*Reflect on the prompt sequence fed to your generation tool. Evaluate why it naturally generated insecure code under your happy-path functional prompt constraints. Detail what security shortcuts the model attempted to take to make the feature execute quickly, and analyze how easily it fell into standard vulnerability traps like BOLA/IDOR, Race Conditions, or input injection.*

> **\[STUDENT MODEL BIAS & CRITIQUE TEXT HERE\]**

\================================================================================

## SECTION 3: COGNITIVE REFLECTION — DOCUMENT AUTHORING & THREAT DESIGN

\================================================================================

### 3.1 AI Collaboration Matrix for Security Analysis

*Detail exactly how you collaborated with your chosen AI tools to construct your written security requirements, abuse cases, attack trees, and threat modeling frameworks.*

I did not use AI when working on the final deliverables for security-analysis.md. I worked on these deliverables based on my working knowledge of the application from code review. For the DFD, I used the example in the module reading as inspiration and paired it with my explanation in Section 2.1 of this document. For the STRIDE matrix table, I consulted the reading as well as online examples. Based on Steve's recommendation, I included IDs throughout and paired them with the mitigations in the document so that there are clear connections. I did attempt to get AI assistance on the attack surface mapping and dataflow diagram/STRIDE documentation originally, but the output it provided did not align with what I believed was expected, so I scrapped it (see AI logs).

Admittedly, this project took considerable time (21 hours as I am writing this), so after having little success with the attack surface mapping, I felt it was better to complete that part of the assignment with my working knowledge rather than wrestle with AI for assistance.

### 3.2 Evaluation of Automated Conceptual "Blind Spots"

*Analyze the output your document-authoring AI generated. Did the tool hallucinate security threat concepts, overlook critical domain-specific logical risks (like over-allocating hardware inventory pools), or suggest generic text summaries that lacked rigorous engineering context? Detail how you manually audited and revised the AI's structural concepts to match real-world parameters.*

As noted above, I did not use AI generated content for my security-analysis.md document. When I did try to prompt it for assistance on the attack surface mapping and DFD/STRIDE deliverables, the recommendation was too far from what I felt was expected for the assignment to warrant using it in any capacity. I also felt I was running low on time, and it would be more resource intensive to interrogate AI work than to document the information myself based on my working knowledge.

\================================================================================

## SECTION 4: HUMAN VERIFICATION & AUDIT TRAIL LOG

\================================================================================ *Record every manual configuration test, integration check, dynamic execution verification (Postman, cURL loops), or tool scan triage loop handled completely by you by hand.*

| Timestamp | Component Target | Testing/Auditing New Methodology | Verified Outcome / Discovered Defect |
| :---- | :---- | :---- | :---- |
| *2026-11-15 09:35:00* | *`GET /auth/csrf`* | *Executed manual Postman request to retrieve CSRF token.* | *Passed check: Received token* |
| *2026-11-15 09:40:00* | *`POST /auth/register`* | *Executed manual Postman request to register a user.* | *Passed check: User created - 201 Created* |
| *2026-11-15 09:42:00* | *`POST /auth/login`* | *Executed manual Postman request to sign in as a registered user.* | *Passed check: Successful sign-in - 200 OK* |
| *2026-11-15 09:45:00* | *`POST /discussions/posts`* | *Executed manual Postman request to create a post.* | *Passed check: Successful post - 201 Created* |
| *2026-11-15 09:49:00* | *`GET /discussions/posts?postId={{postId}}&projectId={{projectId}}`* | *Executed manual Postman request to get a specific post.* | *Passed check: Successful read - 200 OK* |
| *2026-11-15 10:32:00* | *`POST /auth/register`* | *Executed manual Postman request to register an account with a short password (9 characters)* | *Passed check: 400 Bad Request, error "password size must be between 12 and 72" <br> <b>Defect:</b> weak password policy* |
| *2026-11-15 10:35:00* | *`POST /auth/register`* | *Executed manual Postman request to register an account with a malicious username (`<script>alert(1)</script>/`)* | *Passed check: 400 Bad Request, error "username must match \"^[A-Za-z0-9._-]{3,30}$\""* |
| *2026-11-15 11:07:00* | *`POST /discussions/posts`* | *Executed manual Postman request to attempt to provide post id and overwrite an existing post.* | *Passed check: Inclusion of id parameter did not overwrite existing posts* |
| *2026-11-15 11:07:00* | *`GET /discussions/posts`* | *Executed manual Postman request to view all posts.* | *Passed check: All posts returned* |
| *2026-11-15 11:26:00* | *`PUT /discussions/posts/{{postId}}`* | *Executed manual Postman request as an admin to attempt to modify the text of a user's post.* | *<b>Needs review:</b> 200 OK - could be considered a defect when admins are given moderation abilities. Does not follow least privilege and could result in additional impacts if admin account is commandeered.* |
| *2026-11-15 11:26:00* | *`PUT /discussions/posts/{{postId}}`* | *Executed manual Postman request to attempt to include a script tag (`<script>alert(1)</script>/`) in post body.* | *<b>Defect:</b> Received 200 OK. The body of posts and comments expect rich text according to the specs. While the response is not being rendered in HTML for the purposes of this assignment, I think it is important that the body of posts and comments should be sanitized before they reach the database layer, otherwise it could become stored XSS when rendered later on unless each rendering sanitizes the output properly.* |

&nbsp;