package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hackathon.data.enums.TeamEnum;

@Data
public class UpdateTeamDTO {
    @NotBlank(message = "队名不能为空")
    private String name;
    private String introduction;
    @NotNull(message = "队伍类型不能为空")
    private TeamEnum type;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
