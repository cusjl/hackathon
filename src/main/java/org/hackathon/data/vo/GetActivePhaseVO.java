package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.PhaseStatus;

import java.time.LocalDateTime;

@Data
public class GetActivePhaseVO {
    private Boolean existed;
    private Integer phaseId;
    private String name;
    private LocalDateTime submitBeg;
    private LocalDateTime submitEnd;
    private LocalDateTime reviewBeg;
    private LocalDateTime reviewEnd;
    private PhaseStatus status;
    private Long leftDays;
    private Integer leftHours;
    private Integer leftMinutes;
}
