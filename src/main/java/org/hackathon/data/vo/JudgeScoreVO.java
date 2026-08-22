package org.hackathon.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 计分明细中的单个评委得分，counted=false 表示被去极值规则剔除
 */
@Data
public class JudgeScoreVO {
    private Integer assignmentId;
    private Integer judgeId;
    private String judgeName;
    private BigDecimal totalScore;
    private LocalDateTime submitTime;
    private Boolean counted;
    //被剔除的原因：最高分 / 最低分
    private String excludeReason;
}
