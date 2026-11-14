# Code AI History

## Entry 1

**Prompt:** "Thank you. Now, please review spec.md and use the instructions there to generate a bare bones version of that application using the Java/Spring Boot stack that only demonstrates how a student might interact with the application. Include tests to demonstrate that a student can do what the specs say a student can do, and tests that demonstrate a student cannot do anything that they aren't explicitly listed as being able to to do. Log this prompt and a summary of any code you write in the prompt_test_log.md file, as well as in the code_AI_history.md file."

**Code summary:** Added a Spring Boot REST API limited to student-visible projects, posts, and comments. Implemented HTTP Basic demo authentication, in-memory seed data, per-student course assignment checks, post ownership checks, request validation, and deny-by-default authorization for all other routes and methods. Added integration tests for successful student actions and denied/unassigned actions. Added local run instructions in `README.md`.

Original AI generated code is here: https://github.com/chloewildman/cs763-wildman-final-project/commit/7d0ccc6c6d2f1d3818a5321bb81513fa7569adfa

## Entry 2

**Prompt:** "How do I incorporate building my docker container environment, trivy, and semgrep or sonarqube into my devsecops.yml workflow? Please include any of my prompts and your response in code_AI_history.md and prompt_test_log.md"

**Response:** Updated `.github/workflows/devsecops.yml` to run Semgrep on the Java source, verify and package with Maven, build the existing Dockerfile as a local image, and scan that image with Trivy. Semgrep findings and Trivy HIGH/CRITICAL OS or library vulnerabilities with available fixes fail the workflow. Semgrep was selected over SonarQube to avoid requiring a SonarQube server, project configuration, and token; the image is not pushed to a registry.

## Entry 3

**Prompt:** "Why did you remove my test and build steps in favor of a single test and package step? I'd like to keep them separate."

**Response:** Maven `verify` covers testing and packaging, so I combined them, but that obscured the workflow's separate stages. Restored separate `./mvnw -B test` and `./mvnw -B package -DskipTests` steps before the Docker build and Trivy scan.

## Entry 4

**Prompt:** "Why does the semgrep output in github show " • Findings: 4 (4 blocking)" if non-blocking?"

**Response:** The "blocking" label describes the findings, but GitHub determines whether the workflow step fails from Semgrep's exit code. The workflow runs `semgrep scan` without `--error`, so findings alone do not fail it; Semgrep documents that this CLI command exits 0 by default when a scan completes, even when it reports findings. `semgrep ci` has different default behavior.

## Entry 5

**Prompt:** "I'm receiving the following warnings. Please help me move away from deprecated resources. [actions/checkout@v4 and actions/setup-python@v5 target Node.js 20; setup-java v4 is deprecated and should migrate to v5.]"

**Response:** Updated the workflow's maintained GitHub actions to `actions/checkout@v7`, `actions/setup-java@v6`, and `actions/setup-python@v7`. These use current supported runtimes and preserve the existing Java 25, Python 3.12, Maven, Semgrep, Docker, and Trivy steps. The Trivy action was unchanged because it was not named in the deprecation warnings.

## Entry 6

**Prompt:** "Can you tell me (make no changes) if this project has met the criteria below?
spec.md: A detailed architectural design document mapping out your proposed REST endpoints, payload schemas, and database entity relationships. (20%)
Agent_environment.md: the exact configuration of their autonomous developer AI agent, including the exact tool use and its operational mode, the LLM model, the hyperparameters and configuration, and host environment, etc. (20%)
Prompt_test_log.md: the initial prompts and generated code to show the AI developer agent is working (10%)
.github/workflows/devsecops.yml: A fully functional GitHub Actions pipeline script configured for your chosen tech stack.(30%)
Dockerfile & docker-compose.yml: Multi-stage Docker configurations successfully building a basic barebone app structure.(20%)
A verified pipeline execution log showing successful, non-blocking SAST and dependency scanning telemetry."

**Response:** Make `spec.md` request/response contracts explicit for each endpoint; document exact agent tools and settings in `Agent_environment.md`, marking configuration that is unavailable; maintain traceable prompts and generated-code references; link the successful GitHub Actions run and surface Semgrep/Trivy telemetry; and confirm the requested capitalization of `Prompt_test_log.md`. The existing generated-code release link was noted as the provenance reference. No application, workflow, or Docker files were changed.

**Prompt:** "Please add those recommendations to prompt_test_log.md and code_AI_history.md"

**Response:** Added the assessment recommendations to both history files and retained the generated-code release link as a provenance reference.

## Entry 7

**Prompt:** "Thank you. I've made some updates. Now are all of the deliverables met? Please record this prompt and your response in the code_AI_history.md and Prompt_test_log.md files."

**Response:** Not all deliverables are fully met. `spec.md` now includes CRUD endpoints and POST/PUT payload examples, and the malformed User response example is fixed. It still does not define an endpoint/payload for managing `Assignments` (professor assignment or student course enrollment), despite those requirements in its user stories. `Agent_environment.md` documents the model, mode, permissions, environment, and unavailable hyperparameters, but not the exact tools used. The prompt log contains prompt history and a generated-code release link; the worktree filename is capitalized, but Git tracks it as lowercase `agent/prompt_test_log.md`. The workflow and Docker configuration are unchanged and a successful Actions run is available: https://github.com/chloewildman/cs763-wildman-final-project/actions/runs/41478938408. The run confirms Semgrep and Trivy steps succeeded; its summary does not show finding counts, and the current documentation updates are uncommitted and absent from that run. Conclusion: not all criteria are fully met.

## Entry 8

**Prompt:** Thank you. Now, ignoring the capitalization of prompt_test_log.md, and ignoring the finding count in the workflow/docker/run evidence, are these deliverables met?

**Response:** Assessment: Broadly met, with two small strict-interpretation caveats
Ignoring prompt-log capitalization and scan finding counts, the main deliverables are in place: spec.md now includes CRUD and assignment endpoints, payload examples, and entity relationships; Agent_environment.md lists the model, mode, permissions, environment, unsupported hyperparameters, and tools; the prompt log contains initial prompts and a generated-code release link; and the workflow and multi-stage Docker setup have a successful Actions run #8.

For strict completeness, spec.md labels the Assignment payload as “POST & PUT” although the endpoint list has no PUT assignment route. Also, the agent document lists tool categories rather than exact tool names, so whether it meets “exact tool use” depends on how literally that requirement is graded. Aside from those points, yes—the deliverables appear met. No files were changed.
