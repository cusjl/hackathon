package org.hackathon.data.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 管理员把若干评委指派到若干作品，按笛卡尔积建立评审任务，已存在的组合跳过
 */
@Data
public class AssignJudgeDTO {
    @NotEmpty(message = "评委列表不能为空")
    private List<Integer> judgeIds;
    @NotEmpty(message = "作品列表不能为空")
    private List<Integer> submissionIds;
}
