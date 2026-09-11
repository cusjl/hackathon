package org.hackathon.data.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class ShowcaseActionDTO {
    @NotNull @Positive private Integer version;
}
