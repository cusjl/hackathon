package org.hackathon.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 按负载最轻优先自动为本轮每份作品补齐评委
 */
@Data
public class AutoAssignDTO {
    @NotNull(message = "每份作品的评委数不能为空")
    @Positive(message = "每份作品的评委数必须为正数")
    @Max(value = 20, message = "每份作品的评委数不能超过20")
    private Integer judgeCount;
}
