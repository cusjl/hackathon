package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.AssignSource;
import org.hackathon.data.enums.ReviewStatus;
import org.hackathon.data.enums.SubmissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评委待评列表条目。盲审轮次下 teamName 为随机编号且不含任何身份信息。
 */
@Data
public class ReviewTaskVO {
    private Integer assignmentId;
    private Integer submissionId;
    private Integer phaseId;
    private Integer teamId;
    private String teamName;
    private ReviewStatus status;
    private AssignSource source;
    private SubmissionStatus submissionStatus;
    private Integer versionNo;
    private LocalDateTime submitTime;
    //本评委给出的加权总分，未打分为空
    private BigDecimal totalScore;
    private LocalDateTime scoreTime;
    private Integer urgeCount;
    private LocalDateTime lastUrgeTime;
    //该作品上未处理的异常标记数
    private Integer openFlagCount;
}
