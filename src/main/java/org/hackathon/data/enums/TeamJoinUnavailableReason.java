package org.hackathon.data.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The field that makes a student incompatible with a team collaboration rule.
 */
public enum TeamJoinUnavailableReason {
    MAJOR_MISMATCH("专业不一致"),
    CAMPUS_MISMATCH("校区不一致");

    private final String description;

    TeamJoinUnavailableReason(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
