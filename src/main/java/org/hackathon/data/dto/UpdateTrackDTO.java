package org.hackathon.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hackathon.annotation.PatchNotBlank;

@Data
public class UpdateTrackDTO {
    @PatchNotBlank(message = "名称不能为空")
    private String name;
    @PatchNotBlank(message = "描述md不能为空")
    private String descMd;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
