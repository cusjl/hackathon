package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    private Boolean isBlind;
    //中期打卡
    private Boolean hasCheck;
    //手动晋级
    private Boolean isManual;
    private BigDecimal passRate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
