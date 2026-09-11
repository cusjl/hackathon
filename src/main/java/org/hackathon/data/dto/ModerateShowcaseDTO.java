package org.hackathon.data.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class ModerateShowcaseDTO {
    @NotNull @Positive private Integer version;
    @NotNull private Boolean blocked;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
