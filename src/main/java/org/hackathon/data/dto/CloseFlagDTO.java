package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员提前关闭补交窗口
 */
@Data
public class CloseFlagDTO {
    @NotBlank(message = "关闭理由不能为空")
    @Size(max = 200, message = "关闭理由长度不能超过200个字符")
    private String reason;
}
