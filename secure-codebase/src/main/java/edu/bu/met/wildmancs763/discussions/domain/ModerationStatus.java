/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ModerationStatus {
    ACTIVE("active"),
    REPORTED("reported"),
    HIDDEN_BY_MODERATOR("hidden_by_moderator");

    private final String value;

    ModerationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ModerationStatus fromValue(String value) {
        for (ModerationStatus status : values()) {
            if (status.value.equals(value.toLowerCase(Locale.ROOT))) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown moderation status: " + value);
    }
}
