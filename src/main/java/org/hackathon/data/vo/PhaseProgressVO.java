package org.hackathon.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮次打分进度看板
 */
@Data
public class PhaseProgressVO {
    private Integer phaseId;
    private String phaseName;
    private LocalDateTime reviewBeg;
    private LocalDateTime reviewEnd;
    private Boolean reviewClosed;
    private Integer judgeCount;
    private Integer totalTask;
    private Integer doneTask;
    private Integer pendingTask;
    private List<JudgeProgressVO> judges;
}
