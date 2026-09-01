package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryJoinableTeamDTO {
    @NotBlank(message = "队伍名称不能为空")
    @Size(max = 100, message = "队伍名称不能超过100个字符")
    private String name;
}
