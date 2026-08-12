package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
