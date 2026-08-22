package org.hackathon.data.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 单个评分维度的得分与评语
 */
@Data
public class ScoreItemDTO {
    @NotNull(message = "评分维度id不能为空")
    private Integer dimensionId;
    @NotNull(message = "维度得分不能为空")
    @DecimalMin(value = "0.00", message = "维度得分不能为负数")
    private BigDecimal score;
    @Size(max = 500, message = "维度评语长度不能超过500个字符")
    private String comment;
}
