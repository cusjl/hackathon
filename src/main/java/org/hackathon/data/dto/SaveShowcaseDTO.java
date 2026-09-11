package org.hackathon.data.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;

@Data
public class SaveShowcaseDTO {
    @NotNull @Positive private Integer sourceSubmissionId;
    @NotNull @Positive private Integer sourceVersionNo;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String summary;

    @Size(max = 100000)
    private String introMd;

    @Size(max = 500)
    private String repoUrl;

    @Size(max = 50)
    private String licenseType;

    @Size(max = 500)
    private String derivedFrom;

    @Size(max = 500)
    private String demoUrl;

    @Size(max = 500)
    private String videoUrl;

    @Positive private Long coverFileId;
    @Positive private Long videoFileId;

    @Size(max = 20)
    private List<@NotNull @Positive Long> imageFileIds;

    @Positive private Integer version;
}
