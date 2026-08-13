package org.hackathon.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserIdDTO {
    @NotNull(message = "用户id不能为空")
    private Integer userId;
}
