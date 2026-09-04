package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SduPassTestDTO {
    @NotBlank(message = "学号不能为空")
    @Size(max = 12, message = "学号不能超过12位")
    private String casId;
    @NotBlank(message = "姓名不能为空")
    private String name;
}
