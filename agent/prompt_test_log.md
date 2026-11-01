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