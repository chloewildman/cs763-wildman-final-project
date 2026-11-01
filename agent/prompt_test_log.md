# Prompt Test Log

## Entry 1

**Prompt:** "Please review spec.md and provide some suggested improvements that would clarify the requirements of my application. Any time I request something from you, please log my prompt in the prompt_test_log.md file along with a summary of the changes you made."

**Summary of changes:** Reviewed `spec.md` and identified requirement ambiguities and inconsistencies; provided suggested clarifications in the response. No changes were made to `spec.md`; this request and summary were logged here.

## Entry 2

**Prompt:** "Please make the changes that you've observed. Since this is all tracked in git, I consider your changes suggestions which I can reject."

**Summary of changes:** Updated `spec.md` with proposed API behavior, acceptance criteria, and pipeline checks; aligned resource relationships, IDs, timestamps, and mock data; and recorded unresolved authentication, collection, and field-constraint decisions. Logged this request here.

## Entry 3

**Prompt:** "Thank you. Now, please review spec.md and use the instructions there to generate a bare bones version of that application using the Java/Spring Boot stack that only demonstrates how a student might interact with the application. Include tests to demonstrate that a student can do what the specs say a student can do, and tests that demonstrate a student cannot do anything that they aren't explicitly listed as being able to to do. Log this prompt and a summary of any code you write in the prompt_test_log.md file, as well as in the code_AI_history.md file."

**Summary of changes:** Added a Spring Boot student-only REST API with HTTP Basic authentication, in-memory sample data, assigned-project access checks, post creation/editing, and comments. Added integration tests for permitted student flows, ownership and assignment restrictions, and denied operations. Updated demo run instructions and logged this request in both requested history files.

## Entry 4

**Prompt:** "Please review Dockerfile and docker-compose.yml and determine if they are properly configured for my environment."

**Summary of changes:** Reviewed the Dockerfile, Compose file, Docker ignore rules, and Java runtime requirements. Verified that Compose configuration parses on the installed Windows Docker setup and attempted an image build, which fails because Spring Boot 4 does not support the configured `layertools` jarmode. Also identified that Compose does not pass the required `DEMO_STUDENT_PASSWORD` and that the image build skips tests. No Docker configuration changes were made; this review was logged here.

## Entry 5

**Prompt:** "How do I incorporate building my docker container environment, trivy, and semgrep or sonarqube into my devsecops.yml workflow? Please include any of my prompts and your response in code_AI_history.md and prompt_test_log.md"

**Response:** Updated `.github/workflows/devsecops.yml` to run Semgrep on the Java source, verify and package with Maven, build the existing Dockerfile as a local image, and scan that image with Trivy. Semgrep findings and Trivy HIGH/CRITICAL OS or library vulnerabilities with available fixes fail the workflow. Semgrep was selected over SonarQube to avoid requiring a SonarQube server, project configuration, and token; the image is not pushed to a registry.

## Entry 6

**Prompt:** "Why did you remove my test and build steps in favor of a single test and package step? I'd like to keep them separate."

**Response:** Maven `verify` covers testing and packaging, so I combined them, but that obscured the workflow's separate stages. Restored separate `./mvnw -B test` and `./mvnw -B package -DskipTests` steps before the Docker build and Trivy scan.

## Entry 7

**Prompt:** "Why does the semgrep output in github show " • Findings: 4 (4 blocking)" if non-blocking?"

**Response:** The "blocking" label describes the findings, but GitHub determines whether the workflow step fails from Semgrep's exit code. The workflow runs `semgrep scan` without `--error`, so findings alone do not fail it; Semgrep documents that this CLI command exits 0 by default when a scan completes, even when it reports findings. `semgrep ci` has different default behavior.

## Entry 8

**Prompt:** "I'm receiving the following warnings. Please help me move away from deprecated resources. [actions/checkout@v4 and actions/setup-python@v5 target Node.js 20; setup-java v4 is deprecated and should migrate to v5.]"

**Summary of changes:** Updated the workflow's maintained GitHub actions to `actions/checkout@v7`, `actions/setup-java@v6`, and `actions/setup-python@v7`. These use current supported runtimes and preserve the existing Java 25, Python 3.12, Maven, Semgrep, Docker, and Trivy steps. The Trivy action was unchanged because it was not named in the deprecation warnings.

## Entry 9

**Prompt:** "Please review the current modifications I'm drafting in the src/main/objects folder. I am refactoring how this code is designed. Consult my spec.md document which contains an "Assignments" table I want to produce. How do I use Spring Boot, JPA, and these classes to accomplish that?"

**Summary of changes:** Reviewed the draft JPA entities against `spec.md`. Recommended representing `Assignments` as its own entity with an embedded composite key and `@MapsId` relationships because it has a `role` column; aligning `INSTRUCTOR` with the spec's `PROFESSOR`; avoiding a duplicate professor relationship on `Course`; adding repositories and replacing the existing map-backed service; and preserving UTC timestamps with `Instant`. The 11 existing tests pass, but they do not verify assignment persistence. No source changes were made.

## Entry 10

**Prompt:** "How do I configure a connection with SQLite?"

**Summary of changes:** Explained the SQLite JDBC and Hibernate community dialect dependencies for Spring Boot 4.1.2, the datasource URL/driver/dialect properties, use of a persistent file path and Docker volume, and keeping H2 test-only if needed. No application configuration was changed.

## Entry 11

**Prompt:** "In CommentService.java, I am currently using a series of .get() to access the course, which isn't very readable. How would you recommend refactoring this? Perhaps a separate service for authorization?"

**Summary of changes:** Recommended a focused course-authorization service because `CourseService` repeats the membership check, with authorization based on user/course IDs and a repository existence query. For comments, use an authorization-aware repository query or course-id projection instead of traversing `Comment -> Post -> Project -> Course -> Users`; avoid `Collection.contains` on entities without ID-based equality, use constructor injection, and handle missing comments explicitly. No source changes were made.

## Entry 12

**Prompt:** "Why is maven no longer compiling?"

**Summary of changes:** Diagnosed Maven compile errors as test class `EndpointTests.java` being placed in `src/main/java`, where test-scoped Spring test dependencies are unavailable. Recommended moving it to the matching package under `src/test/java`; the archived service file is commented out and is not the cause. No Java files were changed.