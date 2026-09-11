package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import org.hackathon.data.enums.ShowcaseStatus;

import java.time.LocalDateTime;

@Data
public class ShowcaseProject {
    @TableId(type = IdType.AUTO)
    private Integer showcaseId;

    private Integer teamId;
    private Integer eventId;
    private Integer trackId;
    private Integer sourceSubmissionId;
    private Integer sourceVersionNo;
    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String introMd;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String repoUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String licenseType;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String derivedFrom;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String demoUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String videoUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long coverFileId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long videoFileId;

    private ShowcaseStatus status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String moderationReason;

    private LocalDateTime publishedAt;
    @Version private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
