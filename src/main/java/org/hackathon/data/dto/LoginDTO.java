package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "学号/手机号/邮箱不能为空")
    private String term;
    @NotBlank(message = "密码不能为空")
    private String password;
}
