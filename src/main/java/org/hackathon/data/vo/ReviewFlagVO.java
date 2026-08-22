package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.FlagTarget;

import java.time.LocalDateTime;

/**
 * Demo 异常标记与其开出的补交窗口
 */
@Data
public class ReviewFlagVO {
    private Integer flagId;
    private Integer submissionId;
    private FlagTarget target;
    private String description;
    private FlagStatus status;
    private Integer judgeId;
    //盲审视角下不下发标记人姓名
    private String judgeName;
    private LocalDateTime supplementEnd;
    private LocalDateTime supplementOpenTime;
    //窗口当前是否仍然有效
    private Boolean windowOpen;
    private Integer resolvedVersionNo;
    private LocalDateTime resolveTime;
    private String closeReason;
    private LocalDateTime createTime;
    private Integer version;
}
