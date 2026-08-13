package org.hackathon.data.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateExUserDTO {
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50字")
    private String name;
    @NotNull(message = "账号种类不能为空")
    private Boolean onCampus;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    @Size(max = 50, message = "组织名称不能大于50字")
    private String organization;
    @NotEmpty(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6到20位之间")
    private String password;

}
