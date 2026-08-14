package org.hackathon.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PageParamDTO {
    @Positive(message = "页数必须为正数")
    @NotNull(message = "页数数不能为null")
    private Integer page = 1;
    @Positive(message = "单页条数必须为正数")
    @Max(value = 100, message = "单页条数不能超过100条")
    @NotNull(message = "单页条数不能为null")
    private Integer size = 20;
}
