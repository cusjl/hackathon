package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InviteCodeDTO {
    @NotBlank(message = "邀请码不能为空")
    @Pattern(regexp = "[A-Za-z0-9]{12}", message = "邀请码格式错误")
    private String code;
}
