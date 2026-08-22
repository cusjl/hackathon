package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评委工作台的作品评审视图：作品内容 + 本轮评分维度 + 本评委已有打分。
 * 盲审轮次下 blind=true，作品中的队名被替换为随机编号，members 不下发。
 */
@Data
public class ReviewWorkVO {
    private Integer assignmentId;
    private ReviewStatus status;
    private Boolean blind;
    private SubmissionInfoVO submission;
    //非盲审时下发队伍成员，供评委判断是否需要回避
    private List<TeamMemberVO> members;
    private List<DimensionVO> dimensions;
    private List<ReviewScoreVO> scores;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime scoreTime;
    private List<ReviewFlagVO> flags;
    //评审时间窗内才可打分
    private Boolean scorable;
    private Integer version;
}
