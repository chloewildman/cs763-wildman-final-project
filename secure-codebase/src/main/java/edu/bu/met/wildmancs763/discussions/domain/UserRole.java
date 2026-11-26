/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.domain;

public enum UserRole {
    STUDENT,
    STUDENT_PEER,
    FACULTY_INSTRUCTOR,
    FACULTY,
    ADMIN
}
