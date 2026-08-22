package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.SubmissionConfig;
import org.hackathon.security.Role;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum FileScope {

    AVATAR(0, "avatar", Group.AVATAR, true, false, 5, 30, Ext.IMAGE, Read.NONE),
    EVENT_COVER(1, "event/cover", Group.EVENT_ASSET, true, false, 10, 60, Ext.IMAGE, Read.NONE),
    TRACK_ATTACHMENT(2, "track/attach", Group.TRACK_ASSET, true, false, 200, 60, Ext.DOC_ARCHIVE, Read.NONE),

    SUBMIT_ARCHIVE(10, "submit/archive", Group.SUBMIT, false, false, 500, 60, Ext.ARCHIVE, Read.WORK),
    SUBMIT_VIDEO(11, "submit/video", Group.SUBMIT, false, false, 2048, 120, Ext.VIDEO, Read.WORK),
    SUBMIT_DOC(12, "submit/doc", Group.SUBMIT, false, false, 100, 60, Ext.SLIDE, Read.WORK),
    SUBMIT_IMAGE(13, "submit/image", Group.SUBMIT, false, false, 10, 60, Ext.IMAGE, Read.WORK),
    MILESTONE(14, "submit/milestone", Group.SUBMIT, false, false, 50, 60, Ext.DOC_IMAGE, Read.WORK),

    APPEAL(20, "appeal", Group.APPEAL, false, false, 100, 60, Ext.DOC_ARCHIVE, Read.TEAM_SIDE),

    CERT_TEMPLATE(30, "cert/template", Group.EVENT_ASSET, false, false, 10, 60, Ext.IMAGE, Read.ADMIN_ONLY),
    CERT_SEAL(31, "cert/seal", Group.EVENT_ASSET, false, false, 10, 60, Ext.IMAGE, Read.ADMIN_ONLY),
    CERTIFICATE(32, "cert/issued", Group.SYSTEM, false, true, 10, 30, Ext.PDF, Read.OWNER_ADMIN),

    EXPORT(40, "export", Group.SYSTEM, false, true, 200, 30, Ext.EXPORTABLE, Read.ADMIN_ONLY),

    SHOWCASE(50, "showcase", Group.SHOWCASE, true, false, 2048, 120, Ext.IMAGE_VIDEO, Read.NONE),
    ;

    public enum Group { AVATAR, EVENT_ASSET, TRACK_ASSET, SUBMIT, APPEAL, SHOWCASE, SYSTEM }

    public enum Anchor { USER, EVENT, TRACK, TEAM, SUBMISSION }

    @EnumValue
    private final Integer value;
    private final String directory;
    private final Group group;
    private final Boolean guestReadable;
    private final Boolean systemOnly;
    private final Integer maxSizeMB;
    private final Integer urlExpireMinutes;
    private final Set<String> allowedExtensions;
    private final Role[] readRoles;

    FileScope(Integer value, String directory, Group group, Boolean guestReadable, Boolean systemOnly,
              Integer maxSizeMB, Integer urlExpireMinutes, Set<String> allowedExtensions, Role[] readRoles) {
        this.value = value;
        this.directory = directory;
        this.group = group;
        this.guestReadable = guestReadable;
        this.systemOnly = systemOnly;
        this.maxSizeMB = maxSizeMB;
        this.urlExpireMinutes = urlExpireMinutes;
        this.allowedExtensions = allowedExtensions;
        this.readRoles = readRoles;
    }

    private interface Read {
        Role[] NONE = {};
        Role[] WORK = {Role.TEAM_MEMBER, Role.EVENT_JUDGE, Role.EVENT_ADMIN};
        Role[] TEAM_SIDE = {Role.TEAM_MEMBER, Role.EVENT_ADMIN};
        Role[] ADMIN_ONLY = {Role.EVENT_ADMIN};
        Role[] OWNER_ADMIN = {Role.SELF, Role.EVENT_ADMIN};
    }

    private interface Ext {
        Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "webp");
        Set<String> VIDEO = Set.of("mp4", "webm", "mkv", "mov");
        Set<String> ARCHIVE = Set.of("zip", "rar", "7z", "tar", "gz");
        Set<String> SLIDE = Set.of("pdf", "ppt", "pptx");
        Set<String> PDF = Set.of("pdf");
        Set<String> DOC = Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md");
        Set<String> EXPORTABLE = Set.of("xlsx", "csv", "zip");
        Set<String> IMAGE_VIDEO = union(IMAGE, VIDEO);
        Set<String> DOC_IMAGE = union(DOC, IMAGE);
        Set<String> DOC_ARCHIVE = union(DOC, ARCHIVE);

        static Set<String> union(Set<String> a, Set<String> b) {
            return Stream.concat(a.stream(), b.stream()).collect(Collectors.toUnmodifiableSet());
        }
    }

    public Anchor anchor() {
        return switch (group) {
            case AVATAR -> Anchor.USER;
            case EVENT_ASSET -> Anchor.EVENT;
            case TRACK_ASSET -> Anchor.TRACK;
            case SUBMIT, APPEAL -> Anchor.SUBMISSION;
            case SHOWCASE -> Anchor.TEAM;
            case SYSTEM -> this == CERTIFICATE ? Anchor.USER : Anchor.EVENT;
        };
    }

    public boolean allows(String extension) {
        return allowedExtensions.contains(extension);
    }

    public long maxSizeBytes() {
        return maxSizeMB * 1024L * 1024L;
    }

    /**
     * 管理员在本轮配置的体积上限只能收紧、不能放宽枚举自身的固定上限
     */
    public long effectiveMaxBytes(SubmissionConfig config) {
        if (config == null) return maxSizeBytes();
        Integer configured = switch (this) {
            case SUBMIT_ARCHIVE -> config.getMaxSizeMB();
            case SUBMIT_VIDEO -> config.getVideoMaxSizeMB();
            default -> null;
        };
        if (configured == null) return maxSizeBytes();
        return Math.min(maxSizeBytes(), configured * 1024L * 1024L);
    }

    public boolean enabledIn(SubmissionConfig config, Phase phase) {
        if (this == MILESTONE) return Boolean.TRUE.equals(phase.getMidCheck());
        if (config == null) return true;
        return switch (this) {
            case SUBMIT_ARCHIVE -> Boolean.TRUE.equals(config.getZip());
            case SUBMIT_VIDEO -> Boolean.TRUE.equals(config.getVideo());
            case SUBMIT_DOC -> Boolean.TRUE.equals(config.getPowerpoint());
            case SUBMIT_IMAGE -> Boolean.TRUE.equals(config.getMarkdown());
            default -> true;
        };
    }

    private static final Map<String, String> EXT_MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("tar", "application/x-tar"),
            Map.entry("gz", "application/gzip"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("csv", "text/csv"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown")
    );

    public static String mimeOf(String extension) {
        return EXT_MIME.getOrDefault(extension, "application/octet-stream");
    }
}
