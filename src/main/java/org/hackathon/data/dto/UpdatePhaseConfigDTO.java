package org.hackathon.data.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePhaseConfigDTO {
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean repository = false;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean openSource = false;
    private String licenseType;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean zip = false;
    private Integer maxSizeMB;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean video = false;
    private Integer videoMaxSizeMB;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean powerpoint = false;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean website = false;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean markdown = false;
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean declare = false;
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
