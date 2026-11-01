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