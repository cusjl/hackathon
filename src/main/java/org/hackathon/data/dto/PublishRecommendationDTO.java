package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PublishRecommendationDTO {
    @NotBlank(message = "自荐介绍不能为空")
    @Size(max = 1000, message = "自荐介绍不能超过1000字")
    private String introduction;
    @Size(max = 20, message = "技能标签不能超过20个")
    private List<String> skills;
}
