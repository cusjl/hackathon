package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.FlagTarget;

import java.time.LocalDateTime;

/**
 * 评委对作品打出的异常标记，同时是队长「补交窗口」的唯一凭据。
 * 该行完整记录了开窗人、开窗时间、窗口截止时间与最终补交产生的版本号，构成豁免通道的审计痕迹。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewFlag {
    @TableId(type = IdType.AUTO)
    private Integer flagId;
    private Integer submissionId;
    private Integer phaseId;
    //发起标记的评委
    private Integer judgeId;
    private FlagTarget target;
    private String description;
    private FlagStatus status;
    //补交窗口截止时间
    private LocalDateTime supplementEnd;
    //开窗人
    private Integer supplementOpener;
    private LocalDateTime supplementOpenTime;
    //补交产生的作品版本号
    private Integer resolvedVersionNo;
    private LocalDateTime resolveTime;
    private String closeReason;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 窗口当前是否有效：标记未处理且未过窗口截止时间
     */
    public boolean windowOpen(LocalDateTime now) {
        return status == FlagStatus.OPEN && supplementEnd != null && now.isBefore(supplementEnd);
    }
}
