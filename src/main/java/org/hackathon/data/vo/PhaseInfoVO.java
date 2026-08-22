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
    private LocalDateTime pollBeg;
    private LocalDateTime pollEnd;
    //每日票数上限，未配置时为 null，后端按每人每天 3 票执行
    private Integer pollDailyCap;
    private SubmissionConfig submissionConfig;
    private Integer trackId;
    private String trackName;
    private Integer eventId;
    private String eventName;
    private Integer version;
}
