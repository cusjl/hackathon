package org.hackathon.data.po;

import lombok.Data;

@Data
public class SubmissionConfig {
    private Boolean repository;
    private Boolean OpenSource;
    private String licenseType;
    private Boolean zip;
    private Integer maxSizeMB;
    private Boolean video;
    private Boolean maxMinute;
    private Boolean powerpoint;
    private Boolean website;
    private Boolean markdown;
    private Boolean declare;
}
