package org.hackathon.data.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.hackathon.data.enums.PublicMetric;

import java.util.List;

@Data
public class PublicMetricsDTO {
    @NotNull
    @Size(max = 6)
    private List<@NotNull PublicMetric> publicMetrics;

    @NotNull
    @Min(0)
    private Integer version;
}
