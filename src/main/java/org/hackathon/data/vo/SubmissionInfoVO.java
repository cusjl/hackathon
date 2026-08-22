package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.SubmissionStatus;
import org.hackathon.data.po.SubmissionConfig;

import java.time.LocalDateTime;

/**
 * 作品详情，同时承担“动态表单渲染”：
 * config 给出本轮勾选的提交项，其余字段为已提交内容（尚未提交时为 null）。
 */
@Data
public class SubmissionInfoVO {
    private Integer submissionId;
    private Integer phaseId;
    private String phaseName;
    private Integer teamId;
    private String teamName;
    //本轮勾选的提交项，前端据此渲染表单
    private SubmissionConfig config;
    private LocalDateTime submitBeg;
    private LocalDateTime submitEnd;
    //当前是否处于可提交/修改状态，截止后为 false
    private Boolean editable;
    private SubmissionStatus status;
    private Integer versionNo;
    private Integer submitterId;
    private String submitterName;
    private LocalDateTime submitTime;
    private String repoUrl;
    private String licenseType;
    private String derivedFrom;
    private Long archiveFileId;
    private FileInfoVO archive;
    private Long videoFileId;
    private FileInfoVO video;
    private String videoUrl;
    private Long docFileId;
    private FileInfoVO doc;
    private String demoUrl;
    private String introMd;
    private String declaration;
    private Integer version;
}
