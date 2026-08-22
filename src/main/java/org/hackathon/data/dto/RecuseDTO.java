package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评委申请利益回避
 */
@Data
public class RecuseDTO {
    @NotBlank(message = "回避理由不能为空")
    @Size(max = 500, message = "回避理由长度不能超过500个字符")
    private String reason;
}
