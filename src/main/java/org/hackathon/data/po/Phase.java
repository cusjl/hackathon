package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.PhaseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(autoResultMap = true)
public class Phase {
    @TableId(type = IdType.AUTO)
    private Integer phaseId;
    private Integer trackId;
    private String name;
    private LocalDateTime submitBeg;
    private LocalDateTime submitEnd;
    private LocalDateTime reviewBeg;
    private LocalDateTime reviewEnd;
    //盲审
    private Boolean blindReview;
    //中期打卡
    private Boolean midCheck;
    //手动晋级
    private Boolean manualPick;
    private BigDecimal passRate;
    //大众投票
    private Boolean poll;
    @TableField(typeHandler = Jackson3TypeHandler.class)
    private SubmissionConfig submissionConfig;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @SuppressWarnings("unused")
    public PhaseStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(submitBeg)) {
            return PhaseStatus.PREP;
        } else if (now.isBefore(submitEnd)) {
            return PhaseStatus.SUBMIT;
        } else if (now.isBefore(reviewBeg)) {
            return PhaseStatus.MID;
        } else if (now.isBefore(reviewEnd)) {
            return PhaseStatus.REVIEW;
        }
        return PhaseStatus.END;
    }

    @SuppressWarnings("unused")
    public void setStatus(PhaseStatus status) {
    }
}
