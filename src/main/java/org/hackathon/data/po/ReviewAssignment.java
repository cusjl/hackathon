package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.AssignSource;
import org.hackathon.data.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评委 → 具体作品的评审任务，(submission_id, judge_id) 唯一。
 * 回避或移交后原任务置为终态并保留，由系统另建一条任务承接，形成可追溯的分发链。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewAssignment {
    @TableId(type = IdType.AUTO)
    private Integer assignmentId;
    private Integer phaseId;
    private Integer submissionId;
    private Integer judgeId;
    private ReviewStatus status;
    private AssignSource source;
    //指派人，系统自动分发时为空
    private Integer assignerId;
    //由哪条任务回避/移交衍生而来
    private Integer sourceAssignmentId;
    //本评委加权总分，百分制
    private BigDecimal totalScore;
    //本评委总评语
    private String comment;
    private LocalDateTime submitTime;
    private String recuseReason;
    private LocalDateTime recuseTime;
    private Integer urgeCount;
    private LocalDateTime lastUrgeTime;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
