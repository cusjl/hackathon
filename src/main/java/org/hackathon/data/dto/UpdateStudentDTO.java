package org.hackathon.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateStudentDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "校区不能为空")
    @Pattern(
            regexp = "^(中心校区|洪家楼校区|趵突泉校区|千佛山校区|软件园校区|兴隆山校区|威海校区|青岛校区)$",
            message = "校区选择无效"
    )
    private String campus;
    @NotBlank(message = "专业不能为空")
    @Size(max = 50, message = "专业名称过长")
    private String major;
    @Size(max = 100, message = "简介过长")
    private String introduction;
    private List<String> tags;
}
