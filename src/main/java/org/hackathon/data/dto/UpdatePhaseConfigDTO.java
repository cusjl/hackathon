package org.hackathon.data.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePhaseConfigDTO {
    private Boolean repository = false;
    private Boolean OpenSource = false;
    private String licenseType;
    private Boolean zip = false;
    private Integer maxSizeMB;
    private Boolean video = false;
    private Integer maxMinute;
    private Boolean powerpoint = false;
    private Boolean website = false;
    private Boolean markdown = false;
    private Boolean declare = false;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
