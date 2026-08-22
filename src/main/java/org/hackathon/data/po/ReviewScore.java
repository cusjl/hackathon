package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 某条评审任务下某个维度的得分与评语，(assignment_id, dimension_id) 唯一。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewScore {
    @TableId(type = IdType.AUTO)
    private Long scoreId;
    private Integer assignmentId;
    private Integer dimensionId;
    private BigDecimal score;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
