package org.hackathon.security;

import jakarta.servlet.http.HttpServletRequest;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
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
            RegistrationMapper registration
    ) {}

    private static final Set<String> PATH_IDS = Set.of("eventId", "trackId", "phaseId", "teamId", "userId");

    private final LocalJwt jwt;
    private final LocalJwt.Auth auth;
    private final Map<String, Integer> pathIds;
    private final Deps deps;

    private Event event;
    private Track track;
    private Phase phase;
    private Team team;

    private Boolean isSuperCached, isEventAdminCached, isEventJudgeCached, isTeamMemberCached;

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
                hasPhase = pathIds.containsKey("phaseId"), hasTeam = pathIds.containsKey("teamId");

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

    public Phase phase() {
        if (phase != null) return phase;
        Integer id = pathIds.get("phaseId");
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位轮次");
        phase = deps.phase().selectById(id);
        if (phase == null) throw new BusinessException(ResultCode.PHASE_NOT_FOUND);
        return phase;
    }

    public Team team() {
        if (team != null) return team;
        Integer id = pathIds.get("teamId");
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位队伍");
        team = deps.team().selectById(id);
        if (team == null) throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
        return team;
    }

    public Track track() {
        if (track != null) return track;
        Integer id = pathIds.get("trackId");
        if (id == null && pathIds.containsKey("phaseId")) id = phase().getTrackId();
        if (id == null && pathIds.containsKey("teamId"))  id = team().getTrackId();
        if (id == null) throw new BusinessException(ResultCode.PARAM_ERROR, "路径中无法定位赛道");
        track = deps.track().selectById(id);
        if (track == null) throw new BusinessException(ResultCode.TRACK_NOT_FOUND);
        return track;
    }

    public Event event() {
        if (event != null) return event;
        Integer id = pathIds.get("eventId");
        if (id == null) id = pathIds.containsKey("teamId") ? team().getEventId()
                : track().getEventId();
        event = deps.event().selectById(id);
        if (event == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        return event;
    }

    public Optional<Phase> phaseOpt() {
        return pathIds.containsKey("phaseId") ? Optional.of(phase()) : Optional.empty();
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

    boolean isAssignedJudge()   { throw new UnsupportedOperationException("评审模块尚未实现"); }
    boolean isSubmissionOwner() { throw new UnsupportedOperationException("评审模块尚未实现"); }
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
