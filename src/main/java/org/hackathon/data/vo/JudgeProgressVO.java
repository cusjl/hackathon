package org.hackathon.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个评委的打分进度，如「评委A 已完成 8/10」
 */
@Data
public class JudgeProgressVO {
    private Integer judgeId;
    private String judgeName;
    private Integer total;
    private Integer done;
    private Integer pending;
    private Integer recused;
    private Integer transferred;
    //形如 8/10
    private String progress;
    //评审已截止但仍有待评任务
    private Boolean overdue;
    private Integer urgeCount;
    private LocalDateTime lastUrgeTime;
    //仍待评的任务id，供管理员直接发起移交
    private List<Integer> pendingAssignmentIds;
}
