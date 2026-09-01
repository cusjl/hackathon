package org.hackathon.data.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTeamConfigDTO {
    @NotNull(message = "队伍最小人数不能为空")
    @Min(value = 1, message = "队伍最小人数不能小于1")
    private Integer minSize;
    @NotNull(message = "队伍最大人数不能为空")
    @Min(value = 1, message = "队伍最大人数不能小于1")
    @Max(value = 100, message = "队伍最大人数不能超过100")
    private Integer maxSize;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
