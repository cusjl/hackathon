package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTrackDTO {
    @NotBlank(message = "赛道名称不能为空")
    private String name;
    @NotBlank(message = "赛道描述不能为空")
    private String descMd;
}
