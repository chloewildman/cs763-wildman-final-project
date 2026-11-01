# Code AI History

## Entry 1

**Prompt:** "Thank you. Now, please review spec.md and use the instructions there to generate a bare bones version of that application using the Java/Spring Boot stack that only demonstrates how a student might interact with the application. Include tests to demonstrate that a student can do what the specs say a student can do, and tests that demonstrate a student cannot do anything that they aren't explicitly listed as being able to to do. Log this prompt and a summary of any code you write in the prompt_test_log.md file, as well as in the code_AI_history.md file."

**Code summary:** Added a Spring Boot REST API limited to student-visible projects, posts, and comments. Implemented HTTP Basic demo authentication, in-memory seed data, per-student course assignment checks, post ownership checks, request validation, and deny-by-default authorization for all other routes and methods. Added integration tests for successful student actions and denied/unassigned actions. Added local run instructions in `README.md`.