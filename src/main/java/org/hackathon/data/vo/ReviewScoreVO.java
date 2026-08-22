package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 评委在某个维度上的打分回显
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewScoreVO {
    private Integer dimensionId;
    private String name;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private BigDecimal score;
    private String comment;
}
