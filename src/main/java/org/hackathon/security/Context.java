package org.hackathon.security;

import jakarta.servlet.http.HttpServletRequest;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.Track;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.*;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Context {

    public enum View { FULL, BLIND, PUBLIC }

    @Component
    public record Deps(
            AuthorityMapper authority,
            EventMapper event,
            TrackMapper track,
            PhaseMapper phase,
            TeamMapper team,
            RegistrationMapper registration,
            SubmissionMapper submission,
            ReviewAssignmentMapper assignment,
            ReviewFlagMapper flag
    ) {}

    private static final Set<String> PATH_IDS =
            Set.of("eventId", "trackId", "phaseId", "teamId", "userId", "submissionId");

    private final LocalJwt jwt;
    private final LocalJwt.Auth auth;
    private final Map<String, Integer> pathIds;
    private final Deps deps;

    private Event event;
    private Track track;
    private Phase phase;
    private Team team;
    private Submission submission;

    private Boolean isSuperCached, isEventAdminCached, isEventJudgeCached, isTeamMemberCached,
            isAssignedJudgeCached;

    private Context(LocalJwt jwt, LocalJwt.Auth auth, Map<String, Integer> pathIds, Deps deps) {
        this.jwt = jwt; this.auth = auth; this.pathIds = pathIds; this.deps = deps;
    }

    static Context of(HttpServletRequest req, LocalJwtUtils jwtUtils, Deps deps) {
        LocalJwt.Resolved r = jwtUtils.tryResolve(req);
        return new Context(r.jwt(), r.auth(), parsePathIds(req), deps);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> parsePathIds(HttpServletRequest req) {
        Map<String, String> raw = (Map<String, String>) req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Map<String, Integer> ids = new HashMap<>();
        if (raw == null) return ids;
        raw.forEach((k, v) -> {
                    if (!PATH_IDS.contains(k)) return;
                    try {
                        ids.put(k, Integer.parseInt(v));
                    } catch (NumberFormatException e) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, k + " 传入的参数 " + v + " 不合法");
                    }
                }
        );
        return ids;
    }

    public Context anchor(Map<String, Integer> ids) {
        Map<String, Integer> merged = new HashMap<>(pathIds);
        ids.forEach((k, v) -> {
            if (v != null && PATH_IDS.contains(k)) merged.put(k, v);
        });
        return new Context(jwt, auth, merged, deps);
    }

    public boolean isAuthenticated() { return auth == LocalJwt.Auth.OK; }

    public boolean isStudent() { return isAuthenticated() && Boolean.TRUE.equals(jwt.getStudentFlag()); }

    public boolean isExtern() { return isAuthenticated() && !Boolean.TRUE.equals(jwt.getStudentFlag()); }

    public Integer userId() { return jwt == null ? null : jwt.getUserId(); }
    public String name() { return jwt == null ? null : jwt.getName(); }
    public String casId() { return jwt == null ? null : jwt.getCasId(); }
    public Integer pathUserId() { return pathIds.get("userId"); }

    void requireValidCredential() {
        switch (auth) {
            case EXPIRED -> throw new BusinessException(ResultCode.TOKEN_EXPIRED);
            case INVALID -> throw new BusinessException(ResultCode.TOKEN_UNREADABLE);
            default -> {}
        }
    }

    void requireAuthenticated() {
        if (auth == LocalJwt.Auth.ANONYMOUS) {
            throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        }
    }

    void requireConsistentPath() {
        if (pathIds.size() < 2) return;
        boolean hasEvent = pathIds.containsKey("eventId"), hasTrack = pathIds.containsKey("trackId"),
                hasPhase = pathIds.containsKey("phaseId"), hasTeam = pathIds.containsKey("teamId"),
                hasSubmission = pathIds.containsKey("submissionId");

        if (hasSubmission && hasTeam && !submission().getTeamId().equals(pathIds.get("teamId")))
            throw new BusinessException(ResultCode.SUBMISSION_NOT_FOUND);
        if (hasSubmission && hasPhase && !submission().getPhaseId().equals(pathIds.get("phaseId")))
            throw new BusinessException(ResultCode.SUBMISSION_NOT_FOUND);

        if (hasTrack && hasEvent && !track().getEventId().equals(pathIds.get("eventId")))
            throw new BusinessException(ResultCode.TRACK_NOT_FOUND);
        if (hasPhase && hasTrack && !phase().getTrackId().equals(pathIds.get("trackId")))
            throw new BusinessException(ResultCode.PHASE_NOT_FOUND);
        if (hasTeam && hasTrack && !team().getTrackId().equals(pathIds.get("trackId")))
            throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
        if (hasTeam && hasEvent && !team().getEventId().equals(pathIds.get("eventId")))
            throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
        if (hasTeam && hasPhase && !team().getTrackId().equals(phase().getTrackId()))
            throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
    }

    void requireWindow(Require.Window window) {
        LocalDateTime now = LocalDateTime.now();
        switch (window) {
            case ANY -> {}
            case REGISTRATION -> requireBetween(now, event().getRegBeg(), event().getRegEnd(),
                    ResultCode.NOT_REGISTER_TIME);
            case SUBMIT -> requireBetween(now, phase().getSubmitBeg(), phase().getSubmitEnd(),
                    ResultCode.NOT_SUBMIT_TIME);
            case REVIEW -> requireBetween(now, phase().getReviewBeg(), phase().getReviewEnd(),
                    ResultCode.NOT_REVIEW_TIME);
            case VOTE -> requireBetween(now, phase().getPollBeg(), phase().getPollEnd(),
                    ResultCode.NOT_VOTE_TIME);
            case PUBLICITY -> requireBefore(now, phase().getPublicityEnd(), ResultCode.PUBLICITY_CLOSED);
            case SUPPLEMENT -> requireSupplementWindow(now);
        }
    }

    /**
     * 补交窗口：只有当作品上存在评委开出的、尚未处理且未过期的异常标记时才放行。
     * 该窗口独立于 SUBMIT，既不放宽提交时间窗，也不改变作品的锁定状态。
     */
    private void requireSupplementWindow(LocalDateTime now) {
        if (deps.flag().selectOpenWindows(submission().getSubmissionId(), now).isEmpty()) {
            throw new BusinessException(ResultCode.SUPPLEMENT_WINDOW_CLOSED);
        }
    }

    private void requireBetween(LocalDateTime now, LocalDateTime beg, LocalDateTime end, ResultCode code) {
        if (beg == null || end == null || now.isBefore(beg) || now.isAfter(end)) {
            throw new BusinessException(code);
        }
    }

    private void requireBefore(LocalDateTime now, LocalDateTime end, ResultCode code) {
        if (end == null || now.isAfter(end)) {
            throw new BusinessException(code);
        }
    }

    public Submission submission() {
        if (submission != null) return submission;
        Integer id = pathIds.get("submissionId");
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位作品");
        submission = deps.submission().selectById(id);
        if (submission == null) throw new BusinessException(ResultCode.SUBMISSION_NOT_FOUND);
        return submission;
    }

    public Phase phase() {
        if (phase != null) return phase;
        Integer id = pathIds.get("phaseId");
        if (id == null && pathIds.containsKey("submissionId")) id = submission().getPhaseId();
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位轮次");
        phase = deps.phase().selectById(id);
        if (phase == null) throw new BusinessException(ResultCode.PHASE_NOT_FOUND);
        return phase;
    }

    public Team team() {
        if (team != null) return team;
        Integer id = pathIds.get("teamId");
        if (id == null && pathIds.containsKey("submissionId")) id = submission().getTeamId();
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位队伍");
        team = deps.team().selectById(id);
        if (team == null) throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
        return team;
    }

    public Track track() {
        if (track != null) return track;
        Integer id = pathIds.get("trackId");
        if (id == null && hasPhase()) id = phase().getTrackId();
        if (id == null && hasTeam())  id = team().getTrackId();
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位赛道");
        track = deps.track().selectById(id);
        if (track == null) throw new BusinessException(ResultCode.TRACK_NOT_FOUND);
        return track;
    }

    public Event event() {
        if (event != null) return event;
        Integer id = pathIds.get("eventId");
        if (id == null) id = hasTeam() ? team().getEventId() : track().getEventId();
        event = deps.event().selectById(id);
        if (event == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        return event;
    }

    private boolean hasPhase() {
        return pathIds.containsKey("phaseId") || pathIds.containsKey("submissionId");
    }

    private boolean hasTeam() {
        return pathIds.containsKey("teamId") || pathIds.containsKey("submissionId");
    }

    public Optional<Phase> phaseOpt() {
        return hasPhase() ? Optional.of(phase()) : Optional.empty();
    }

    @SuppressWarnings("uncheck")
    public boolean is(Role role) { return role.test(this); }

    boolean isSelf() {
        return isAuthenticated() && Objects.equals(pathUserId(), userId());
    }

    boolean isSuper() {
        if (isSuperCached == null) {
            isSuperCached = deps.authority().existsSuper(userId());
        }
        return isSuperCached;
    }

    boolean isEventAdmin() {
        if (isEventAdminCached == null) {
            isEventAdminCached = deps.authority().existsEventAdmin(userId(), event().getEventId());
        }
        return isEventAdminCached;
    }

    boolean isEventJudge() {
        if (isEventJudgeCached == null) {
            isEventJudgeCached = deps.authority().existsEventJudge(userId(), event().getEventId());
        }
        return isEventJudgeCached;
    }

    boolean isTeamLeader() {
        return isAuthenticated() && team().getLeaderId().equals(userId());
    }

    boolean isTeamMember() {
        if (isTeamMemberCached == null) {
            isTeamMemberCached = deps.registration().existsTeamMember(userId(), team().getTeamId());
        }
        return isTeamMemberCached;
    }

    /**
     * 被指派到该作品、且任务仍由其持有的评委。回避或移交之后判定即失效。
     */
    boolean isAssignedJudge() {
        if (isAssignedJudgeCached == null) {
            isAssignedJudgeCached = isEventJudge()
                    && deps.assignment().existsHeld(submission().getSubmissionId(), userId());
        }
        return isAssignedJudgeCached;
    }

    /**
     * 作品归属队伍的成员（含队长）。作品定位后队伍即随之确定，故复用队员判定。
     */
    boolean isSubmissionOwner() {
        submission();
        return isTeamMember();
    }

    boolean isCertOwner()       { throw new UnsupportedOperationException("证书模块尚未实现"); }

    public View view() {
        return phaseOpt().map(this::view).orElse(isAuthenticated() ? View.FULL : View.PUBLIC);
    }

    public View view(Phase p) {
        if (!isAuthenticated()) return View.PUBLIC;
        if (Boolean.TRUE.equals(p.getBlindReview()) && isEventJudge() && !isEventAdmin()) {
            return View.BLIND;
        }
        return View.FULL;
    }
}
