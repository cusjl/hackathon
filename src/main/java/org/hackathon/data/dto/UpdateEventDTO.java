package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventDTO {
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotNull(message = "报名开始日期不能为空")
    private LocalDateTime regBeg;
    @NotNull(message = "报名结束日期不能为空")
    private LocalDateTime regEnd;
    @NotNull(message = "赛事开始日期不能为空")
    private LocalDateTime liveBeg;
    @NotNull(message = "赛事结束日期不能为空")
    private LocalDateTime liveEnd;
    private String introduction;
    private String tags;
    private String notice;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
