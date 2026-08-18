package org.hackathon.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum Role {

    LOGGED_IN(Context::isAuthenticated),
    STUDENT(Context::isStudent),
    EXTERN(Context::isExtern),
    SELF(Context::isSelf),

    SUPER(Context::isSuper),
    EVENT_ADMIN(Context::isEventAdmin),
    EVENT_JUDGE(Context::isEventJudge),

    TEAM_MEMBER(Context::isTeamMember),
    TEAM_LEADER(Context::isTeamLeader),

    ASSIGNED_JUDGE(Context::isAssignedJudge),
    SUBMISSION_OWNER(Context::isSubmissionOwner),
    CERT_OWNER(Context::isCertOwner);

    private final Predicate<Context> rule;

    public boolean test(Context c) {
        return rule.test(c);
    }
}
