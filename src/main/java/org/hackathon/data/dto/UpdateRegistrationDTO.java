package org.hackathon.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRegistrationDTO {
    @NotNull(message = "赛道id不能为空")
    private Integer trackId;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
