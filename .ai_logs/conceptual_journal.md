# MASTER CONCEPTUAL JOURNAL & AI TELEMETRY LOG

* **Student Name:** Chloe Wildman
* **Assigned Domain Module:** Domain 4: The Research Blog & Project Discussion Board
* **Core Technology Stack:** Java/Spring Boot

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

Of note, the AI generated code also included an endpoint that was not requested to allow PATCH requests to moderation endpoints by authenticated admins and faculty. I assume this is to meet the requirement, "Provide an administrative moderation workflow to enforce community standards across text inputs. The service must support state transitions (active, reported, hidden_by_moderator) applied independently to either root posts or child comments, allowing faculty roles to flag inputs and trigger cascading visibility adjustments across the board."


### 2.2 Defensive Edge-Case Evaluation (Line-by-Line Code Review)

*Perform a line-by-line human audit of the generated source code. Identify where the AI tool completely failed to account for edge cases, unusual boundaries, or unexpected system properties prior to running your pipeline scanners. (e.g., What happens if an integer balance value scales to a negative integer? How does the routing loop handle an invalid JSON payload sequence?)*

Authentication: Weak password policy. Requires at least 12 characters, but no diversity in characters/symbols.

Authorization: Anyone can register -- very little consideration for access control despite the context of course projects and roles. UserAccountService.java persistUser() allows for username enumeration. Since anyone can register for an account, this becomes more of a security concern. GET requests under /api/v1/discussions/ have the .permitAll() rule, so authentication isn't required to view discussions about projects.

Input: User input is not sanitized before it is returned in the response.

Config: Check application.properties for SESSION_COOKIE_SECURE=false, meaning session cookie might not always be encrypted through HTTPS. Health/info actuator endpoints are exposed and could help with basic reconaissance.


### 2.3 The AI Agent's "Security Sins" (Evaluating Code Deficiencies)

*Reflect on the prompt sequence fed to your generation tool. Evaluate why it naturally generated insecure code under your happy-path functional prompt constraints. Detail what security shortcuts the model attempted to take to make the feature execute quickly, and analyze how easily it fell into standard vulnerability traps like BOLA/IDOR, Race Conditions, or input injection.*

> **\[STUDENT MODEL BIAS & CRITIQUE TEXT HERE\]**

\================================================================================

## SECTION 3: COGNITIVE REFLECTION — DOCUMENT AUTHORING & THREAT DESIGN

\================================================================================

### 3.1 AI Collaboration Matrix for Security Analysis

*Detail exactly how you collaborated with your chosen AI tools to construct your written security requirements, abuse cases, attack trees, and threat modeling frameworks.*

> **\[STUDENT DOCUMENT PROMPTING NARRATIVE HERE\]** *Describe the sequencing of prompts used to generate the security text. Did you utilize role-playing, constraint-based prompting, or multi-turn chaining?*

### 3.2 Evaluation of Automated Conceptual "Blind Spots"

*Analyze the output your document-authoring AI generated. Did the tool hallucinate security threat concepts, overlook critical domain-specific logical risks (like over-allocating hardware inventory pools), or suggest generic text summaries that lacked rigorous engineering context? Detail how you manually audited and revised the AI's structural concepts to match real-world parameters.*

> **\[STUDENT CONCEPTUAL AUDIT TEXT HERE\]**

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