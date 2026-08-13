package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.po.SubmissionConfig;

import java.time.LocalDateTime;

@Data
public class PhaseInfoVO {
    private Integer phaseId;
    private String name;
    private LocalDateTime submitBeg;
    private LocalDateTime submitEnd;
    private LocalDateTime reviewBeg;
    private LocalDateTime reviewEnd;
    private Boolean blindReview;
    private Boolean midCheck;
    private Boolean poll;
    private SubmissionConfig submissionConfig;
    private Integer trackId;
    private String trackName;
    private Integer eventId;
    private String eventName;
    private Integer version;
}
