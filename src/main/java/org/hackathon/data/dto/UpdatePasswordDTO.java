package org.hackathon.data.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordDTO {
    @Size(min = 6, max = 20, message = "密码长度必须在6到20位之间")
    private String oldPassword;
    @NotEmpty(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20位之间")
    private String newPassword;
}
