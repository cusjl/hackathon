package org.hackathon.data.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改轮次评分维度，本轮已有评委完成打分后不再允许修改
 */
@Data
public class UpdateDimensionDTO {
    @NotBlank(message = "维度名称不能为空")
    @Size(max = 50, message = "维度名称长度不能超过50个字符")
    private String name;
    @Size(max = 200, message = "评分说明长度不能超过200个字符")
    private String description;
    @NotNull(message = "维度满分不能为空")
    @DecimalMin(value = "0.01", message = "维度满分必须大于0")
    @DecimalMax(value = "999.99", message = "维度满分不能超过999.99")
    private BigDecimal maxScore;
    @NotNull(message = "维度权重不能为空")
    @DecimalMin(value = "0.01", message = "维度权重必须大于0")
    @DecimalMax(value = "999.99", message = "维度权重不能超过999.99")
    private BigDecimal weight;
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer sortNo = 0;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
