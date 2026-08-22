package org.hackathon.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评委按本轮配置的评分维度逐项打分并填写评语，可在评审时间窗内重复提交覆盖
 */
@Data
public class ScoreWorkDTO {
    @Valid
    @NotEmpty(message = "维度得分不能为空")
    private List<ScoreItemDTO> scores;
    @Size(max = 1000, message = "总评语长度不能超过1000个字符")
    private String comment;
    //乐观锁版本号，首次打分可不传
    private Integer version;
}
