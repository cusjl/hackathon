package org.hackathon.data.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员把逾期评委的任务移交给其他评委，不指定接手人时由系统按负载最轻原则挑选
 */
@Data
public class TransferAssignmentDTO {
    private Integer toJudgeId;
    @Size(max = 200, message = "移交说明长度不能超过200个字符")
    private String reason;
}
