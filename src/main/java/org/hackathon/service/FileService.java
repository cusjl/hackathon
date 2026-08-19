package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.config.S3Properties;
import org.hackathon.data.dto.UploadDTO;
import org.hackathon.data.enums.FileScope;
import org.hackathon.data.enums.FileStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.FileObject;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.SubmissionConfig;
import org.hackathon.data.vo.FileInfoVO;
import org.hackathon.data.vo.FileUrlVO;
import org.hackathon.data.vo.UploadVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.FileObjectMapper;
import org.hackathon.security.Context;
import org.hackathon.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileObjectMapper fileObjectMapper;
    private final StorageService storageService;
    private final S3Properties props;

    @Transactional
    public UploadVO presign(UploadDTO dto, FileScope.Group group, Context ctx) {
        FileScope scope = dto.getScope();
        if (scope.getGroup() != group) {
            throw new BusinessException(ResultCode.FILE_SCOPE_MISMATCH);
        }
        if (Boolean.TRUE.equals(scope.getSystemOnly())) {
            throw new BusinessException(ResultCode.FILE_SYSTEM_ONLY);
        }

        String extension = extensionOf(dto.getFilename());
        if (!scope.allows(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED,
                    "该用途仅支持：" + String.join("、", scope.getAllowedExtensions()));
        }

        long limit = limitOf(scope, ctx);
        if (dto.getSizeBytes() > limit) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "文件不得超过 " + limit / 1024 / 1024 + " MB");
        }

        FileObject file = new FileObject();
        file.setObjectKey(buildKey(scope, extension));
        file.setScope(scope);
        file.setStatus(FileStatus.PENDING);
        file.setOriginName(dto.getFilename());
        file.setContentType(FileScope.mimeOf(extension));
        file.setSizeBytes(dto.getSizeBytes());
        file.setUploaderId(ctx.userId());
        applyAnchors(file, scope, ctx);
        LocalDateTime now = LocalDateTime.now();
        file.setCreateTime(now);
        file.setUpdateTime(now);
        fileObjectMapper.insert(file);

        Duration duration = Duration.ofMinutes(props.putExpireMinutes());
        String url = storageService.presignPut(file.getObjectKey(), file.getContentType(), duration);
        return new UploadVO(file.getFileId(), url, file.getContentType(), limit, now.plus(duration));
    }

    @Transactional
    public FileInfoVO complete(Long fileId, Context ctx) {
        FileObject file = mustFind(fileId);
        if (file.getStatus() == FileStatus.READY) return toInfo(file);
        if (file.getStatus() == FileStatus.DELETED) {
            throw new BusinessException(ResultCode.FILE_NOT_READY);
        }
        requireWriter(file, ctx);

        HeadObjectResponse head = storageService.head(file.getObjectKey())
                .orElseThrow(() -> new BusinessException(ResultCode.FILE_NOT_UPLOADED));

        long limit = limitOf(file.getScope(), ctx.anchor(file.anchors()));
        if (head.contentLength() > limit) {
            storageService.delete(file.getObjectKey());
            fileObjectMapper.deleteById(fileId);
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "文件不得超过 " + limit / 1024 / 1024 + " MB");
        }

        file.setStatus(FileStatus.READY);
        file.setSizeBytes(head.contentLength());
        file.setEtag(head.eTag());
        file.setUpdateTime(LocalDateTime.now());
        fileObjectMapper.updateById(file);
        return toInfo(file);
    }

    public FileUrlVO url(Long fileId, boolean download, Context ctx) {
        FileObject file = mustReadable(fileId, ctx);
        String name = download ? displayName(file, ctx) : null;
        Duration duration = Duration.ofMinutes(file.getScope().getUrlExpireMinutes());
        String url = storageService.presignGet(file.getObjectKey(), file.getContentType(), name, duration);
        return new FileUrlVO(url, LocalDateTime.now().plus(duration));
    }

    public FileInfoVO info(Long fileId, Context ctx) {
        return toInfo(mustReadable(fileId, ctx));
    }

    @Transactional
    public void delete(Long fileId, Context ctx) {
        FileObject file = mustFind(fileId);
        if (file.getStatus() == FileStatus.DELETED) return;
        requireWriter(file, ctx);
        file.setStatus(FileStatus.DELETED);
        file.setUpdateTime(LocalDateTime.now());
        fileObjectMapper.updateById(file);
    }

    @Transactional
    public FileObject putSystemFile(FileScope scope, String filename, byte[] content,
                                    Map<String, Integer> anchors, Integer uploaderId) {
        String extension = extensionOf(filename);
        if (!scope.allows(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        FileObject file = new FileObject();
        file.setObjectKey(buildKey(scope, extension));
        file.setScope(scope);
        file.setOriginName(filename);
        file.setContentType(FileScope.mimeOf(extension));
        file.setSizeBytes((long) content.length);
        file.setUploaderId(uploaderId);
        file.setUserId(anchors.get("userId"));
        file.setEventId(anchors.get("eventId"));
        file.setTrackId(anchors.get("trackId"));
        file.setPhaseId(anchors.get("phaseId"));
        file.setTeamId(anchors.get("teamId"));
        file.setEtag(storageService.put(file.getObjectKey(), file.getContentType(), content));
        file.setStatus(FileStatus.READY);
        LocalDateTime now = LocalDateTime.now();
        file.setCreateTime(now);
        file.setUpdateTime(now);
        fileObjectMapper.insert(file);
        return file;
    }

    private FileObject mustFind(Long fileId) {
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null) throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        return file;
    }

    private FileObject mustReadable(Long fileId, Context ctx) {
        FileObject file = mustFind(fileId);
        if (file.getStatus() != FileStatus.READY) {
            throw new BusinessException(ResultCode.FILE_NOT_READY);
        }
        if (Boolean.TRUE.equals(file.getScope().getGuestReadable())) return file;
        if (!ctx.isAuthenticated()) throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        if (file.getUploaderId().equals(ctx.userId())) return file;

        Context anchored = ctx.anchor(file.anchors());
        if (test(anchored, Role.SUPER)) return file;
        for (Role role : file.getScope().getReadRoles()) {
            if (test(anchored, role)) return file;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    private void requireWriter(FileObject file, Context ctx) {
        if (file.getUploaderId().equals(ctx.userId())) return;
        Context anchored = ctx.anchor(file.anchors());
        if (test(anchored, Role.EVENT_ADMIN) || test(anchored, Role.SUPER)) return;
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    private boolean test(Context ctx, Role role) {
        try {
            return role.test(ctx);
        } catch (BusinessException | UnsupportedOperationException e) {
            return false;
        }
    }

    private long limitOf(FileScope scope, Context ctx) {
        if (scope.getGroup() != FileScope.Group.SUBMIT) return scope.maxSizeBytes();
        Phase phase = ctx.phase();
        SubmissionConfig config = phase.getSubmissionConfig();
        if (!scope.enabledIn(config, phase)) {
            throw new BusinessException(ResultCode.SUBMIT_ITEM_DISABLED);
        }
        return scope.effectiveMaxBytes(config);
    }

    private void applyAnchors(FileObject file, FileScope scope, Context ctx) {
        switch (scope.anchor()) {
            case USER -> file.setUserId(ctx.userId());
            case EVENT -> file.setEventId(ctx.event().getEventId());
            case TRACK -> {
                file.setTrackId(ctx.track().getTrackId());
                file.setEventId(ctx.event().getEventId());
            }
            case TEAM -> {
                file.setTeamId(ctx.team().getTeamId());
                file.setTrackId(ctx.track().getTrackId());
                file.setEventId(ctx.event().getEventId());
            }
            case SUBMISSION -> {
                file.setPhaseId(ctx.phase().getPhaseId());
                file.setTeamId(ctx.team().getTeamId());
                file.setTrackId(ctx.track().getTrackId());
                file.setEventId(ctx.event().getEventId());
            }
        }
    }

    private String buildKey(FileScope scope, String extension) {
        LocalDate today = LocalDate.now();
        return "%s/%d/%02d/%s.%s".formatted(
                scope.getDirectory(), today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""), extension);
    }

    private String extensionOf(String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new BusinessException(ResultCode.FILE_NAME_ILLEGAL);
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED, "文件名缺少扩展名");
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private String displayName(FileObject file, Context ctx) {
        if (file.getPhaseId() == null) return file.getOriginName();
        Context anchored = ctx.anchor(file.anchors());
        boolean blind;
        try {
            blind = anchored.view(anchored.phase()) == Context.View.BLIND;
        } catch (BusinessException e) {
            blind = false;
        }
        if (!blind) return file.getOriginName();
        return "Team-%03d-%s.%s".formatted(
                file.getTeamId(),
                file.getScope().getDirectory().replace('/', '-'),
                file.extension());
    }

    private FileInfoVO toInfo(FileObject file) {
        return new FileInfoVO(file.getFileId(), file.getScope(), file.getOriginName(),
                file.getContentType(), file.getSizeBytes(), file.getStatus(), file.getCreateTime());
    }
}
