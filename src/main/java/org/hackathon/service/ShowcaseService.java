package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowcaseService {
    private final ShowcaseProjectMapper projects;
    private final SubmissionMapper submissions;
    private final SubmissionVersionMapper versions;
    private final FileObjectMapper files;
    private final EventMapper events;
    private final TrackMapper tracks;
    private final TeamMapper teams;

    @Transactional(readOnly = true)
    public IPage<Map<String, Object>> list(
            Integer eventId,
            Integer trackId,
            String keyword,
            ShowcaseStatus status,
            PageParamDTO page,
            boolean admin) {
        if (eventId != null && events.selectById(eventId) == null)
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        if (trackId != null) {
            Track track = tracks.selectById(trackId);
            if (track == null || (eventId != null && !eventId.equals(track.getEventId())))
                throw new BusinessException(ResultCode.TRACK_NOT_FOUND);
        }
        if (keyword != null && keyword.length() > 100)
            throw new BusinessException(ResultCode.PARAM_ERROR);
        LambdaQueryWrapper<ShowcaseProject> query =
                new LambdaQueryWrapper<ShowcaseProject>()
                        .eq(eventId != null, ShowcaseProject::getEventId, eventId)
                        .eq(trackId != null, ShowcaseProject::getTrackId, trackId);
        if (!admin)
            query.eq(ShowcaseProject::getStatus, ShowcaseStatus.PUBLISHED)
                    .apply(
                            "event_id IN (SELECT event_id FROM event WHERE live_end <= {0})",
                            LocalDateTime.now());
        else query.eq(status != null, ShowcaseProject::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            String search =
                    keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            query.and(
                    q ->
                            q.like(ShowcaseProject::getTitle, search)
                                    .or()
                                    .like(ShowcaseProject::getSummary, search));
        }
        query.orderByDesc(ShowcaseProject::getPublishedAt, ShowcaseProject::getShowcaseId);
        IPage<ShowcaseProject> result =
                projects.selectPage(new Page<>(page.getPage(), page.getSize()), query);
        if (result.getRecords().isEmpty())
            return new Page<Map<String, Object>>(page.getPage(), page.getSize(), result.getTotal());
        List<ShowcaseProject> records = result.getRecords();
        Map<Integer, Event> eventMap =
                events
                        .selectByIds(
                                records.stream()
                                        .map(ShowcaseProject::getEventId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Event::getEventId, Function.identity()));
        Map<Integer, Track> trackMap =
                tracks
                        .selectByIds(
                                records.stream()
                                        .map(ShowcaseProject::getTrackId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Track::getTrackId, Function.identity()));
        Map<Integer, Team> teamMap =
                teams
                        .selectByIds(
                                records.stream()
                                        .map(ShowcaseProject::getTeamId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Team::getTeamId, Function.identity()));
        return result.convert(
                p -> {
                    Map<String, Object> row =
                            brief(
                                    p,
                                    eventMap.get(p.getEventId()).getName(),
                                    trackMap.get(p.getTrackId()).getName(),
                                    teamMap.get(p.getTeamId()).getName());
                    if (admin) {
                        row.put("status", p.getStatus());
                        row.put("version", p.getVersion());
                        row.put("moderationReason", p.getModerationReason());
                    }
                    return row;
                });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publicDetail(Integer id) {
        ShowcaseProject p = projects.selectById(id);
        if (p == null
                || p.getStatus() != ShowcaseStatus.PUBLISHED
                || LocalDateTime.now().isBefore(events.selectById(p.getEventId()).getLiveEnd()))
            throw new BusinessException(ResultCode.SHOWCASE_NOT_FOUND);
        return detail(p);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> manage(Context ctx) {
        ShowcaseProject p =
                projects.selectOne(
                        new LambdaQueryWrapper<ShowcaseProject>()
                                .eq(ShowcaseProject::getTeamId, ctx.team().getTeamId()));
        if (p == null) return null;
        return management(p);
    }

    @Transactional
    public Map<String, Object> save(SaveShowcaseDTO dto, Context ctx) {
        ended(ctx);
        projects.lockTeam(ctx.team().getTeamId());
        ShowcaseProject p = projects.lockByTeam(ctx.team().getTeamId());
        boolean creating = p == null;
        if (creating) {
            if (dto.getVersion() != null) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            p = new ShowcaseProject();
            p.setTeamId(ctx.team().getTeamId());
            p.setEventId(ctx.event().getEventId());
            p.setTrackId(ctx.track().getTrackId());
            p.setStatus(ShowcaseStatus.DRAFT);
            p.setVersion(1);
            p.setCreateTime(LocalDateTime.now());
        } else {
            version(p, dto.getVersion());
            if (p.getStatus() == ShowcaseStatus.PUBLISHED
                    || p.getStatus() == ShowcaseStatus.BLOCKED)
                throw new BusinessException(
                        ResultCode.SHOWCASE_STATE_CONFLICT, "请先撤回已发布项目；管理员下架项目须先解除下架");
        }
        Submission source = submissions.selectById(dto.getSourceSubmissionId());
        if (source == null || !source.getTeamId().equals(p.getTeamId()))
            throw new BusinessException(ResultCode.SHOWCASE_SOURCE_MISMATCH);
        SubmissionVersion sourceVersion =
                versions.selectByVersionNo(source.getSubmissionId(), dto.getSourceVersionNo());
        if (sourceVersion == null)
            throw new BusinessException(ResultCode.SUBMISSION_VERSION_NOT_FOUND);
        // 导入由前端显式填写：保存接口是展示内容全量替换，不暗中读取后来更新的参赛正文。
        p.setSourceSubmissionId(source.getSubmissionId());
        p.setSourceVersionNo(sourceVersion.getVersionNo());
        p.setTitle(dto.getTitle().trim());
        p.setSummary(trim(dto.getSummary()));
        p.setIntroMd(trim(dto.getIntroMd()));
        p.setRepoUrl(url(dto.getRepoUrl()));
        p.setLicenseType(trim(dto.getLicenseType()));
        p.setDerivedFrom(trim(dto.getDerivedFrom()));
        p.setDemoUrl(url(dto.getDemoUrl()));
        p.setVideoUrl(url(dto.getVideoUrl()));
        p.setCoverFileId(dto.getCoverFileId());
        p.setVideoFileId(dto.getVideoFileId());
        if (p.getVideoFileId() != null && p.getVideoUrl() != null)
            throw new BusinessException(ResultCode.PARAM_ERROR, "演示视频只能提供文件或链接其一");
        Map<Long, String> references = references(p, dto.getImageFileIds());
        validateFiles(p, references, sourceVersion.getSnapshot());
        p.setUpdateTime(LocalDateTime.now());
        if (creating) projects.insert(p);
        else if (projects.updateById(p) == 0)
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        projects.clearFiles(p.getShowcaseId());
        for (var entry : references.entrySet())
            projects.addFile(p.getShowcaseId(), entry.getKey(), entry.getValue());
        projects.log(p.getShowcaseId(), ctx.userId(), "SAVE", null, LocalDateTime.now());
        return management(p);
    }

    @Transactional
    public Map<String, Object> publish(ShowcaseActionDTO dto, Context ctx) {
        ended(ctx);
        ShowcaseProject p = teamProject(ctx);
        version(p, dto.getVersion());
        if (p.getStatus() != ShowcaseStatus.DRAFT && p.getStatus() != ShowcaseStatus.WITHDRAWN)
            throw new BusinessException(ResultCode.SHOWCASE_STATE_CONFLICT);
        if (!StringUtils.hasText(p.getIntroMd())
                || !StringUtils.hasText(p.getRepoUrl())
                || !StringUtils.hasText(p.getLicenseType())
                || (p.getVideoFileId() == null && !StringUtils.hasText(p.getVideoUrl())))
            throw new BusinessException(ResultCode.PARAM_ERROR, "发布需提供架构介绍、仓库地址、协议及演示视频");
        Map<Long, String> references = new TreeMap<>();
        for (var file : projects.files(p.getShowcaseId()))
            references.put(((Number) file.get("fileId")).longValue(), (String) file.get("role"));
        validateFiles(
                p,
                references,
                versions.selectByVersionNo(p.getSourceSubmissionId(), p.getSourceVersionNo())
                        .getSnapshot());
        p.setStatus(ShowcaseStatus.PUBLISHED);
        p.setPublishedAt(LocalDateTime.now());
        update(p, ctx, "PUBLISH", null);
        return management(p);
    }

    @Transactional
    public Map<String, Object> withdraw(ShowcaseActionDTO dto, Context ctx) {
        ShowcaseProject p = teamProject(ctx);
        version(p, dto.getVersion());
        if (p.getStatus() != ShowcaseStatus.PUBLISHED)
            throw new BusinessException(ResultCode.SHOWCASE_STATE_CONFLICT);
        p.setStatus(ShowcaseStatus.WITHDRAWN);
        update(p, ctx, "WITHDRAW", null);
        return management(p);
    }

    @Transactional
    public Map<String, Object> moderate(Integer id, ModerateShowcaseDTO dto, Context ctx) {
        ShowcaseProject p = projects.lock(id);
        if (p == null || !p.getEventId().equals(ctx.event().getEventId()))
            throw new BusinessException(ResultCode.SHOWCASE_NOT_FOUND);
        version(p, dto.getVersion());
        if (dto.getBlocked()) {
            if (p.getStatus() == ShowcaseStatus.BLOCKED)
                throw new BusinessException(ResultCode.SHOWCASE_STATE_CONFLICT);
            p.setStatus(ShowcaseStatus.BLOCKED);
            p.setModerationReason(dto.getReason().trim());
        } else {
            if (p.getStatus() != ShowcaseStatus.BLOCKED)
                throw new BusinessException(ResultCode.SHOWCASE_STATE_CONFLICT);
            p.setStatus(ShowcaseStatus.WITHDRAWN);
            p.setModerationReason(null);
        }
        update(p, ctx, dto.getBlocked() ? "BLOCK" : "UNBLOCK", dto.getReason().trim());
        return management(p);
    }

    private void ended(Context ctx) {
        if (LocalDateTime.now().isBefore(ctx.event().getLiveEnd()))
            throw new BusinessException(ResultCode.SHOWCASE_NOT_ENDED);
    }

    private ShowcaseProject teamProject(Context ctx) {
        ShowcaseProject p = projects.lockByTeam(ctx.team().getTeamId());
        if (p == null) throw new BusinessException(ResultCode.SHOWCASE_NOT_FOUND);
        return p;
    }

    private void version(ShowcaseProject p, Integer version) {
        if (!Objects.equals(p.getVersion(), version))
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
    }

    private void update(ShowcaseProject p, Context ctx, String action, String reason) {
        p.setUpdateTime(LocalDateTime.now());
        if (projects.updateById(p) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        projects.log(p.getShowcaseId(), ctx.userId(), action, reason, LocalDateTime.now());
    }

    private Map<Long, String> references(ShowcaseProject p, List<Long> images) {
        Map<Long, String> refs = new TreeMap<>();
        if (images != null) for (Long id : images) refs.put(id, "IMAGE");
        if (p.getCoverFileId() != null) refs.put(p.getCoverFileId(), "COVER");
        if (p.getVideoFileId() != null && refs.put(p.getVideoFileId(), "VIDEO") != null)
            throw new BusinessException(ResultCode.PARAM_ERROR, "同一文件不能同时作为图片和视频");
        return refs;
    }

    private void validateFiles(
            ShowcaseProject p, Map<Long, String> refs, SubmissionSnapshot source) {
        // 与删除接口按 file_id 顺序锁定同一文件行，避免发布与删除竞态。
        for (var ref : refs.entrySet()) {
            FileObject file = files.lockById(ref.getKey());
            if (file == null) throw new BusinessException(ResultCode.FILE_NOT_FOUND);
            if (file.getStatus() != FileStatus.READY)
                throw new BusinessException(ResultCode.FILE_NOT_READY);
            if (!p.getTeamId().equals(file.getTeamId())
                    || !p.getEventId().equals(file.getEventId()))
                throw new BusinessException(ResultCode.SUBMISSION_FILE_MISMATCH);
            boolean video = ref.getValue().equals("VIDEO");
            boolean imported =
                    video
                            && file.getScope() == FileScope.SUBMIT_VIDEO
                            && Objects.equals(file.getFileId(), source.getVideoFileId());
            if (file.getScope() != FileScope.SHOWCASE && !imported)
                throw new BusinessException(ResultCode.FILE_SCOPE_MISMATCH);
            if (!file.getContentType().startsWith(video ? "video/" : "image/"))
                throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private Map<String, Object> brief(
            ShowcaseProject p, String eventName, String trackName, String teamName) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("showcaseId", p.getShowcaseId());
        out.put("eventId", p.getEventId());
        out.put("eventName", eventName);
        out.put("trackId", p.getTrackId());
        out.put("trackName", trackName);
        out.put("teamId", p.getTeamId());
        out.put("teamName", teamName);
        out.put("title", p.getTitle());
        out.put("summary", p.getSummary());
        out.put("coverFileId", p.getCoverFileId());
        out.put("licenseType", p.getLicenseType());
        out.put("publishedAt", p.getPublishedAt());
        return out;
    }

    private Map<String, Object> detail(ShowcaseProject p) {
        Map<String, Object> out =
                brief(
                        p,
                        events.selectById(p.getEventId()).getName(),
                        tracks.selectById(p.getTrackId()).getName(),
                        teams.selectById(p.getTeamId()).getName());
        out.put("introMd", p.getIntroMd());
        out.put("repoUrl", p.getRepoUrl());
        out.put("derivedFrom", p.getDerivedFrom());
        out.put("demoUrl", p.getDemoUrl());
        out.put("videoUrl", p.getVideoUrl());
        out.put("videoFileId", p.getVideoFileId());
        out.put("files", projects.files(p.getShowcaseId()));
        return out;
    }

    private Map<String, Object> management(ShowcaseProject p) {
        Map<String, Object> out = detail(p);
        out.put("status", p.getStatus());
        out.put("version", p.getVersion());
        out.put("sourceSubmissionId", p.getSourceSubmissionId());
        out.put("sourceVersionNo", p.getSourceVersionNo());
        out.put("moderationReason", p.getModerationReason());
        out.put("updateTime", p.getUpdateTime());
        out.put("operations", projects.logs(p.getShowcaseId()));
        return out;
    }

    private String trim(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String url(String raw) {
        String value = trim(raw);
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if ((!"http".equalsIgnoreCase(uri.getScheme())
                            && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "网址必须为有效的 HTTP/HTTPS 地址且不包含账号密码");
        }
        return value;
    }
}
