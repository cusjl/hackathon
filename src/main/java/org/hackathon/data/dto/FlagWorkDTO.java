package org.hackathon.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hackathon.data.enums.FlagTarget;

/**
 * 评委对损坏的 Demo 链接或异常文件打异常标记，同时为队长开启补交窗口
 */
@Data
public class FlagWorkDTO {
    @NotNull(message = "异常对象不能为空")
    private FlagTarget target;
    @NotBlank(message = "异常信息不能为空")
    @Size(max = 500, message = "异常信息长度不能超过500个字符")
    private String description;
    //补交窗口时长（小时），不传按默认值，且不会越过本轮评审截止时间
    @Positive(message = "补交窗口时长必须为正数")
    @Max(value = 72, message = "补交窗口时长不能超过72小时")
    private Integer windowHours;
}
