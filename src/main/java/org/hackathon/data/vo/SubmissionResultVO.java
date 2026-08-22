package org.hackathon.data.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作品最终得分。评委数 ≥5 时按去极值平均计分：剔除一个最高分与一个最低分后取平均。
 */
@Data
public class SubmissionResultVO {
    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    private Integer assignedCount;
    private Integer scoredCount;
    //是否触发去极值规则
    private Boolean trimmed;
    private String rule;
    private BigDecimal finalScore;
    private List<JudgeScoreVO> judgeScores;
}
