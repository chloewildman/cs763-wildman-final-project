/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.domain;

import edu.bu.met.wildmancs763.discussions.api.InvalidRequestException;

import java.util.Locale;

public enum ScopeType {
    PROJECT,
    COURSE,
    LAB;

    public static ScopeType fromPath(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "projects" -> PROJECT;
            case "courses" -> COURSE;
            case "labs" -> LAB;
            default -> throw new InvalidRequestException(
                    "scopeType must be projects, courses, or labs.");
        };
    }

    public String pathValue() {
        return name().toLowerCase(Locale.ROOT) + "s";
    }
}
