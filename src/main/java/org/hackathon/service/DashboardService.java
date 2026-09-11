package org.hackathon.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.hackathon.data.dto.*;
import org.hackathon.data.enums.*;
import org.hackathon.data.po.*;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.*;
import org.hackathon.security.Context;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DashboardMapper data;
    private final DashboardConfigMapper configs;
    private final PhaseMapper phases;
    private final TrackMapper tracks;
    private static final List<String> CAMPUSES =
            List.of("中心校区", "洪家楼校区", "趵突泉校区", "千佛山校区", "软件园校区", "兴隆山校区", "威海校区", "青岛校区");

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> config(Context ctx) {
        return configuration(ctx.event().getEventId());
    }

    @Transactional
    public Map<String, Object> saveConfig(PublicMetricsDTO dto, Context ctx) {
        Integer eventId = ctx.event().getEventId();
        configs.lockEvent(eventId);
        DashboardConfig config = configs.selectById(eventId);
        if (!Objects.equals(dto.getVersion(), config == null ? 0 : config.getVersion()))
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        if (new HashSet<>(dto.getPublicMetrics()).size() != dto.getPublicMetrics().size())
            throw new BusinessException(ResultCode.PARAM_ERROR, "公开统计项不能重复");
        boolean create = config == null;
        if (create) {
            config = new DashboardConfig();
            config.setEventId(eventId);
            config.setVersion(1);
        }
        config.setPublicMetrics(dto.getPublicMetrics().stream().sorted().toList());
        config.setUpdateTime(LocalDateTime.now());
        if (create) configs.insert(config);
        else if (configs.updateById(config) == 0)
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        return configuration(eventId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> publicDashboard(Context ctx) {
        Event event = ctx.event();
        DashboardConfig config = configs.selectById(event.getEventId());
        List<PublicMetric> selected = config == null ? List.of() : config.getPublicMetrics();
        Map<String, Object> out = base(event);
        out.put("publicMetrics", selected);
        out.put("metrics", metrics(event.getEventId(), selected));
        return out;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> adminDashboard(Context ctx) {
        Event event = ctx.event();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> out = base(event);
        out.put("generatedAt", now);
        out.put("publicConfig", configuration(event.getEventId()));
        out.put("metrics", metrics(event.getEventId(), List.of(PublicMetric.values())));
        out.put("registration", data.registrationSummary(event.getEventId()));
        List<Map<String, Object>> phaseRows = data.phases(event.getEventId());
        for (Map<String, Object> p : phaseRows) {
            boolean available = number(p.get("denominatorAvailable")) == 1;
            long known = number(p.get("knownExpectedTeams")),
                    submitted = number(p.get("submittedTeams"));
            p.put("denominatorAvailable", available);
            p.put("knownMissingTeams", Math.max(0, known - submitted));
            p.put("expectedTeams", available ? known : null);
            p.put("missingTeams", available ? Math.max(0, known - submitted) : null);
            p.put("submissionRate", available ? ratio(submitted, known) : null);
            p.put("denominatorReason", available ? null : "HISTORICAL_ELIGIBILITY_UNAVAILABLE");
            long done = number(p.get("doneTasks")), pending = number(p.get("pendingTasks"));
            p.put("totalTasks", done + pending);
            p.put("reviewRate", ratio(done, done + pending));
        }
        out.put("phases", phaseRows);
        List<Map<String, Object>> judges = data.judges(event.getEventId(), now);
        for (var judge : judges)
            judge.put(
                    "completionRate", ratio(number(judge.get("done")), number(judge.get("total"))));
        out.put("judges", judges);
        out.put("workItems", data.workSummary(event.getEventId(), now));
        return out;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public IPage<Map<String, Object>> details(
            DashboardDetailKind kind, Integer phaseId, PageParamDTO page, Context ctx) {
        Integer eventId = ctx.event().getEventId();
        boolean phaseRequired =
                Set.of(
                                DashboardDetailKind.SUBMITTED,
                                DashboardDetailKind.MISSING_SUBMISSION,
                                DashboardDetailKind.UNASSIGNED)
                        .contains(kind);
        if (phaseRequired && phaseId == null)
            throw new BusinessException(ResultCode.PARAM_ERROR, "此明细需指定phaseId");
        if (phaseId != null) {
            Phase phase = phases.selectById(phaseId);
            if (phase == null
                    || !tracks.selectById(phase.getTrackId()).getEventId().equals(eventId))
                throw new BusinessException(ResultCode.PHASE_NOT_FOUND);
            if (kind == DashboardDetailKind.UNTEAMED || kind == DashboardDetailKind.UNDERFILLED)
                throw new BusinessException(ResultCode.PARAM_ERROR, "报名和队伍明细不接受轮次筛选");
        }
        return data.details(
                new Page<>(page.getPage(), page.getSize()),
                eventId,
                kind,
                phaseId,
                LocalDateTime.now());
    }

    private Map<String, Object> configuration(Integer eventId) {
        DashboardConfig config = configs.selectById(eventId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", eventId);
        out.put("publicMetrics", config == null ? List.of() : config.getPublicMetrics());
        out.put("version", config == null ? 0 : config.getVersion());
        out.put("updateTime", config == null ? null : config.getUpdateTime());
        return out;
    }

    private Map<String, Object> base(Event event) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", event.getEventId());
        out.put("eventName", event.getName());
        out.put("generatedAt", LocalDateTime.now());
        return out;
    }

    private Map<String, Object> metrics(Integer eventId, List<PublicMetric> selected) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (PublicMetric metric : selected)
            switch (metric) {
                case PARTICIPANT_COUNT ->
                        result.put("participantCount", data.participants(eventId));
                case TEAM_COUNT -> result.put("teamCount", data.teams(eventId));
                case SUBMISSION_COUNT -> result.put("submissionCount", data.submissions(eventId));
                case TRACK_SUBMISSIONS ->
                        result.put("trackSubmissions", data.trackSubmissions(eventId));
                case CAMPUS_DISTRIBUTION -> result.put("campusDistribution", campuses(eventId));
                case AI_TOOLS -> result.put("aiTools", wordCloud(eventId, true));
                case TECH_STACKS -> result.put("techStacks", wordCloud(eventId, false));
            }
        return result;
    }

    private List<Map<String, Object>> campuses(Integer eventId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        CAMPUSES.forEach(c -> counts.put(c, 0L));
        counts.put("未知校区", 0L);
        for (var row : data.campuses(eventId)) {
            String campus = (String) row.get("campus");
            counts.merge(
                    campus != null && CAMPUSES.contains(campus) ? campus : "未知校区",
                    number(row.get("count")),
                    Long::sum);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts.entrySet().stream()
                .map(
                        entry -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("campus", entry.getKey());
                            row.put("count", entry.getValue());
                            row.put("ratio", ratio(entry.getValue(), total));
                            return row;
                        })
                .toList();
    }

    private Map<String, Object> wordCloud(Integer eventId, boolean ai) {
        Map<String, Object> result = new LinkedHashMap<>(data.tagCoverage(eventId, ai));
        List<Map<String, Object>> words = data.words(eventId, ai);
        result.put(
                "coverage",
                ratio(number(result.get("reportedProjects")), number(result.get("totalProjects"))));
        result.put("words", words.stream().limit(50).toList());
        result.put("truncated", words.size() > 50);
        return result;
    }

    private long number(Object n) {
        return n instanceof Boolean b ? (b ? 1 : 0) : n == null ? 0 : ((Number) n).longValue();
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0
                ? null
                : BigDecimal.valueOf(numerator)
                        .divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }
}
