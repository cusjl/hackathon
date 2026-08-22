package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 大众投票记录。同一用户对同一作品仅一票（唯一键约束），每日票数按 vote_date 限额。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer phaseId;
    private Integer submissionId;
    //投票自然日，按天限额的计数依据
    private LocalDate voteDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
