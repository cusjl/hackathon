package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QueryInviteCandidateDTO {
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50字")
    private String userName;
}
