package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTrackDTO {
    @NotBlank(message = "赛道名称不能为空")
    private String name;
    @NotBlank(message = "赛道描述不能为空")
    private String descMd;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
