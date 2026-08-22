package org.hackathon.data.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 一键催办，评委列表为空时催办本轮全部仍有待评任务的评委
 */
@Data
public class UrgeJudgeDTO {
    private List<Integer> judgeIds;
    @Size(max = 200, message = "催办附言长度不能超过200个字符")
    private String message;
}
