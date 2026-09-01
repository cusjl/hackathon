package org.hackathon.data.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRecruitmentDTO {
    @NotBlank(message = "招募标题不能为空")
    @Size(max = 100, message = "招募标题不能超过100字")
    private String title;
    @Size(max = 1000, message = "招募说明不能超过1000字")
    private String description;
    @Size(max = 20, message = "招募技能标签不能超过20个")
    private List<String> requiredTags;
    @NotNull(message = "招募人数不能为空")
    @Min(value = 1, message = "招募人数不能小于1")
    private Integer vacancies;
}
