package org.hackathon.data.po;

import lombok.Data;

@Data
public class SubmissionConfig {
    private Boolean repository = false;
    private Boolean openSource = false;
    private String licenseType;
    private Boolean zip = false;
    private Integer maxSizeMB;
    private Boolean video = false;
    private Integer videoMaxSizeMB;
    private Boolean powerpoint = false;
    private Boolean website = false;
    private Boolean markdown = false;
    private Boolean declare = false;
}
