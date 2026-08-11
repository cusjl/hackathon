package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SduPassTestDTO {
    @NotBlank(message = "学号不能为空")
    private String casId;
    @NotBlank(message = "姓名不能为空")
    private String name;
}
