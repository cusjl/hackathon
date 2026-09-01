package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.SubmitWorkDTO;
import org.hackathon.data.enums.FileScope;
import org.hackathon.data.enums.FileStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.enums.SubmissionStatus;
import org.hackathon.data.po.*;
import org.hackathon.data.vo.FileInfoVO;
import org.hackathon.data.vo.SubmissionIdVO;
import org.hackathon.data.vo.SubmissionInfoVO;
import org.hackathon.data.vo.SubmissionVersionBriefVO;
import org.hackathon.data.vo.SubmissionVersionVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.FileObjectMapper;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.SubmissionVersionMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.Context;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final Pattern URL = Pattern.compile("^https?://\\S+$");

    private final SubmissionMapper submissionMapper;
    private final SubmissionVersionMapper versionMapper;
    private final FileObjectMapper fileObjectMapper;
    private final UserMapper userMapper;
    private final ReviewFlagService reviewFlagService;

    /**
     * 提交/重新提交作品，每次提交生成一条版本日志
     * @param dto 作品内容，必填项由本轮 submissionConfig 决定
     * @param ctx 已由 @Require 完成队长与提交时间窗校验
     * @return 作品id与本次版本号
     */
    @Transactional
    public SubmissionIdVO submit(SubmitWorkDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        Team team = ctx.team();
        if (ctx.event().getTeamMinSize() != null && team.getSize() < ctx.event().getTeamMinSize()) {
            throw new BusinessException(ResultCode.TEAM_TOO_SMALL);
        }
        SubmissionSnapshot snapshot = verify(dto, phase, team);

        LocalDateTime now = LocalDateTime.now();
        Submission submission = submissionMapper.selectByTeamPhase(team.getTeamId(), phase.getPhaseId());
        if (submission == null) {
            submission = new Submission();
            submission.setPhaseId(phase.getPhaseId());
            submission.setTeamId(team.getTeamId());
            submission.setVersionNo(1);
            submission.setVersion(1);
            submission.setCreateTime(now);
            fill(submission, snapshot, ctx.userId(), now);
            submissionMapper.insert(submission);
        } else {
            //截止后即锁定，定时任务未跑到时由派生状态兜底
            if (submission.derivedStatus(phase) == SubmissionStatus.LOCKED) {
                throw new BusinessException(ResultCode.SUBMISSION_LOCKED);
            }
            if (dto.getVersion() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "版本号不能为空");
            }
            submission.setVersion(dto.getVersion());
            submission.setVersionNo(submission.getVersionNo() + 1);
            fill(submission, snapshot, ctx.userId(), now);
            if (submissionMapper.updateById(submission) == 0) {
                throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            }
        }

        versionMapper.insert(new SubmissionVersion(null, submission.getSubmissionId(),
                submission.getVersionNo(), snapshot, dto.getChangeLog(), ctx.userId(), now, now));
        return new SubmissionIdVO(submission.getSubmissionId(), submission.getVersionNo());
    }

    /**
     * 补交：评委打出异常标记后开出的受限豁免通道。
     * <p>与常规提交完全隔离——不放宽 SUBMIT 时间窗、不解除作品锁定状态，
     * 仅在 {@link org.hackathon.security.Require.Window#SUPPLEMENT} 校验通过（存在未过期的异常标记）时放行；
     * 内容校验、文件归属校验与版本日志与常规提交一致，补交同样生成新版本，
     * 并把版本号回写到异常标记上作为审计痕迹。
     * @param dto 作品内容，必填项仍由本轮 submissionConfig 决定
     * @param ctx 已由 @Require(value = TEAM_LEADER, window = SUPPLEMENT) 校验
     * @return 作品id与本次补交产生的版本号
     */
    @Transactional
    public SubmissionIdVO supplement(SubmitWorkDTO dto, Context ctx) {
        Phase phase = ctx.phase();
        Team team = ctx.team();
        Submission submission = ctx.submission();
        if (!StringUtils.hasText(dto.getChangeLog())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "补交必须填写提交说明");
        }
        if (dto.getVersion() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "版本号不能为空");
        }
        SubmissionSnapshot snapshot = verify(dto, phase, team);

        LocalDateTime now = LocalDateTime.now();
        submission.setVersion(dto.getVersion());
        submission.setVersionNo(submission.getVersionNo() + 1);
        //补交不改变作品的提交/锁定状态，仅替换内容
        submission.apply(snapshot);
        submission.setSubmitterId(ctx.userId());
        submission.setSubmitTime(now);
        submission.setUpdateTime(now);
        if (submissionMapper.updateById(submission) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }

        String changeLog = "【补交】" + dto.getChangeLog().trim();
        versionMapper.insert(new SubmissionVersion(null, submission.getSubmissionId(),
                submission.getVersionNo(), snapshot,
                changeLog.length() > 200 ? changeLog.substring(0, 200) : changeLog,
                ctx.userId(), now, now));
        reviewFlagService.resolve(submission.getSubmissionId(), submission.getVersionNo(),
                team.getEventId());
        return new SubmissionIdVO(submission.getSubmissionId(), submission.getVersionNo());
    }

    /**
     * 按轮次+队伍查询作品，兼作动态表单渲染的数据源
     */
    public SubmissionInfoVO getSubmission(Context ctx) {
        Phase phase = ctx.phase();
        Team team = ctx.team();
        return toInfo(submissionMapper.selectByTeamPhase(team.getTeamId(), phase.getPhaseId()),
                phase, team, ctx);
    }

    /**
     * 按作品id查询作品
     */
    public SubmissionInfoVO getSubmissionById(Context ctx) {
        return toInfo(ctx.submission(), ctx.phase(), ctx.team(), ctx);
    }

    /**
     * 版本列表，按 v1、v2 … 升序
     */
    public List<SubmissionVersionBriefVO> listVersions(Context ctx) {
        List<SubmissionVersion> versions = versionMapper.selectBySubmission(ctx.submission().getSubmissionId());
        Map<Integer, String> names = nameOf(ctx, versions.stream()
                .map(SubmissionVersion::getSubmitterId).distinct().toList());
        return versions.stream().map(po -> new SubmissionVersionBriefVO(
                po.getVersionNo(), po.getSubmitterId(), names.get(po.getSubmitterId()),
                po.getSubmitTime(), po.getChangeLog()
        )).toList();
    }

    /**
     * 某个历史版本的完整快照
     */
    public SubmissionVersionVO getVersion(Integer versionNo, Context ctx) {
        Submission submission = ctx.submission();
        SubmissionVersion version = versionMapper.selectByVersionNo(submission.getSubmissionId(), versionNo);
        if (version == null) {
            throw new BusinessException(ResultCode.SUBMISSION_VERSION_NOT_FOUND);
        }
        return new SubmissionVersionVO(submission.getSubmissionId(), version.getVersionNo(),
                version.getSubmitterId(), nameOf(ctx, List.of(version.getSubmitterId())).get(version.getSubmitterId()),
                version.getSubmitTime(), version.getChangeLog(), version.getSnapshot());
    }

    // ==================== 动态校验 ====================

    /**
     * 按本轮 submissionConfig 逐项校验：勾选项必填且格式合法，未勾选项拒绝携带
     * @return 校验通过后的作品快照
     */
    private SubmissionSnapshot verify(SubmitWorkDTO dto, Phase phase, Team team) {
        SubmissionConfig config = phase.getSubmissionConfig();
        if (config == null) {
            throw new BusinessException(ResultCode.SUBMIT_ITEM_DISABLED, "本轮尚未配置提交项");
        }
        SubmissionSnapshot snapshot = new SubmissionSnapshot();

        //代码仓库地址
        if (on(config.getRepository())) {
            snapshot.setRepoUrl(requireUrl(dto.getRepoUrl(), "代码仓库地址"));
        } else {
            reject(dto.getRepoUrl(), "代码仓库地址");
        }

        //开源许可协议 + 二次开发原项目说明
        if (on(config.getOpenSource())) {
            String license = require(dto.getLicenseType(), "开源许可协议");
            if (StringUtils.hasText(config.getLicenseType()) && !allowedLicense(config, license)) {
                throw new BusinessException(ResultCode.LICENSE_NOT_ALLOWED,
                        "本轮仅接受协议：" + config.getLicenseType());
            }
            snapshot.setLicenseType(license);
            snapshot.setDerivedFrom(trimToNull(dto.getDerivedFrom()));
        } else {
            reject(dto.getLicenseType(), "开源许可协议");
            reject(dto.getDerivedFrom(), "二次开发原项目说明");
        }

        //源码压缩包
        if (on(config.getZip())) {
            snapshot.setArchiveFileId(requireFile(dto.getArchiveFileId(), FileScope.SUBMIT_ARCHIVE,
                    phase, team, "源码压缩包", FileScope.SUBMIT_ARCHIVE.effectiveMaxBytes(config)));
        } else {
            reject(dto.getArchiveFileId(), "源码压缩包");
        }

        //演示视频：文件或链接二选一
        if (on(config.getVideo())) {
            boolean hasFile = dto.getVideoFileId() != null;
            boolean hasUrl = StringUtils.hasText(dto.getVideoUrl());
            if (!hasFile && !hasUrl) {
                throw new BusinessException(ResultCode.SUBMISSION_ITEM_REQUIRED, "演示视频为必填提交项");
            }
            if (hasFile && hasUrl) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "演示视频只能提供文件或链接其一");
            }
            if (hasFile) {
                snapshot.setVideoFileId(requireFile(dto.getVideoFileId(), FileScope.SUBMIT_VIDEO,
                        phase, team, "演示视频", FileScope.SUBMIT_VIDEO.effectiveMaxBytes(config)));
            } else {
                snapshot.setVideoUrl(requireUrl(dto.getVideoUrl(), "演示视频链接"));
            }
        } else {
            reject(dto.getVideoFileId(), "演示视频");
            reject(dto.getVideoUrl(), "演示视频");
        }

        //演示文档 PPT/PDF
        if (on(config.getPowerpoint())) {
            snapshot.setDocFileId(requireFile(dto.getDocFileId(), FileScope.SUBMIT_DOC,
                    phase, team, "演示文档", FileScope.SUBMIT_DOC.maxSizeBytes()));
        } else {
            reject(dto.getDocFileId(), "演示文档");
        }

        //在线 Demo 网址
        if (on(config.getWebsite())) {
            snapshot.setDemoUrl(requireUrl(dto.getDemoUrl(), "在线Demo网址"));
        } else {
            reject(dto.getDemoUrl(), "在线Demo网址");
        }

        //项目详细介绍 Markdown
        if (on(config.getMarkdown())) {
            snapshot.setIntroMd(require(dto.getIntroMd(), "项目详细介绍"));
        } else {
            reject(dto.getIntroMd(), "项目详细介绍");
        }

        //开源及 AI 声明
        if (on(config.getDeclare())) {
            snapshot.setDeclaration(require(dto.getDeclaration(), "开源及AI声明"));
        } else {
            reject(dto.getDeclaration(), "开源及AI声明");
        }
        return snapshot;
    }

    private boolean on(Boolean flag) {
        return Boolean.TRUE.equals(flag);
    }

    private boolean allowedLicense(SubmissionConfig config, String license) {
        return Arrays.stream(config.getLicenseType().split(","))
                .map(String::trim)
                .anyMatch(license::equalsIgnoreCase);
    }

    private String require(String value, String item) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.SUBMISSION_ITEM_REQUIRED, item + "为必填提交项");
        }
        return value.trim();
    }

    private String requireUrl(String value, String item) {
        String url = require(value, item);
        if (!URL.matcher(url).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, item + "必须是 http/https 链接");
        }
        return url;
    }

    /**
     * 未勾选的提交项不接受任何内容，避免绕过配置写入
     */
    private void reject(Object value, String item) {
        boolean blank = value == null || (value instanceof String s && !StringUtils.hasText(s));
        if (!blank) {
            throw new BusinessException(ResultCode.SUBMIT_ITEM_DISABLED, "本轮未开启" + item);
        }
    }

    /**
     * 文件类提交项校验：必须已上传完成、用途匹配、且属于本队本轮
     */
    private Long requireFile(Long fileId, FileScope scope, Phase phase, Team team,
                             String item, long maxBytes) {
        if (fileId == null) {
            throw new BusinessException(ResultCode.SUBMISSION_ITEM_REQUIRED, item + "为必填提交项");
        }
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        if (file.getStatus() != FileStatus.READY) {
            throw new BusinessException(ResultCode.FILE_NOT_READY);
        }
        if (file.getScope() != scope) {
            throw new BusinessException(ResultCode.FILE_SCOPE_MISMATCH, item + "的文件用途不匹配");
        }
        if (!phase.getPhaseId().equals(file.getPhaseId()) || !team.getTeamId().equals(file.getTeamId())) {
            throw new BusinessException(ResultCode.SUBMISSION_FILE_MISMATCH);
        }
        if (file.getSizeBytes() > maxBytes) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    item + "不得超过 " + maxBytes / 1024 / 1024 + " MB");
        }
        return fileId;
    }

    // ==================== 装配 ====================

    private void fill(Submission submission, SubmissionSnapshot snapshot,
                      Integer submitterId, LocalDateTime now) {
        submission.apply(snapshot);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmitterId(submitterId);
        submission.setSubmitTime(now);
        submission.setUpdateTime(now);
    }

    private SubmissionInfoVO toInfo(Submission submission, Phase phase, Team team, Context ctx) {
        boolean blind = ctx.view(phase) == Context.View.BLIND;
        SubmissionInfoVO vo = new SubmissionInfoVO();
        vo.setPhaseId(phase.getPhaseId());
        vo.setPhaseName(phase.getName());
        vo.setTeamId(team.getTeamId());
        vo.setTeamName(blind ? "Team-%03d".formatted(team.getTeamId()) : team.getName());
        vo.setConfig(phase.getSubmissionConfig());
        vo.setSubmitBeg(phase.getSubmitBeg());
        vo.setSubmitEnd(phase.getSubmitEnd());
        LocalDateTime now = LocalDateTime.now();
        vo.setEditable(phase.getSubmitBeg() != null && phase.getSubmitEnd() != null
                && !now.isBefore(phase.getSubmitBeg()) && !now.isAfter(phase.getSubmitEnd()));
        if (submission == null) return vo;

        vo.setSubmissionId(submission.getSubmissionId());
        vo.setStatus(submission.derivedStatus(phase));
        vo.setVersionNo(submission.getVersionNo());
        vo.setSubmitterId(submission.getSubmitterId());
        vo.setSubmitterName(blind ? null
                : nameOf(ctx, List.of(submission.getSubmitterId())).get(submission.getSubmitterId()));
        vo.setSubmitTime(submission.getSubmitTime());
        vo.setRepoUrl(submission.getRepoUrl());
        vo.setLicenseType(submission.getLicenseType());
        vo.setDerivedFrom(submission.getDerivedFrom());
        vo.setArchiveFileId(submission.getArchiveFileId());
        vo.setArchive(fileInfo(submission.getArchiveFileId()));
        vo.setVideoFileId(submission.getVideoFileId());
        vo.setVideo(fileInfo(submission.getVideoFileId()));
        vo.setVideoUrl(submission.getVideoUrl());
        vo.setDocFileId(submission.getDocFileId());
        vo.setDoc(fileInfo(submission.getDocFileId()));
        vo.setDemoUrl(submission.getDemoUrl());
        vo.setIntroMd(submission.getIntroMd());
        vo.setDeclaration(submission.getDeclaration());
        vo.setVersion(submission.getVersion());
        return vo;
    }

    private FileInfoVO fileInfo(Long fileId) {
        if (fileId == null) return null;
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null) return null;
        return new FileInfoVO(file.getFileId(), file.getScope(), file.getOriginName(),
                file.getContentType(), file.getSizeBytes(), file.getStatus(), file.getCreateTime());
    }

    /**
     * 盲审视角下不返回提交人姓名
     */
    private Map<Integer, String> nameOf(Context ctx, List<Integer> userIds) {
        if (userIds.isEmpty() || ctx.view() == Context.View.BLIND) return Map.of();
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
