package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 轮次的评分维度，由赛事管理员配置，评委按维度逐项打分。
 * 评委总分 = Σ(该维度得分 / 该维度满分 × 权重) / Σ权重 × 100，归一到百分制。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDimension {
    @TableId(type = IdType.AUTO)
    private Integer dimensionId;
    private Integer phaseId;
    private String name;
    private String description;
    //该维度满分
    private BigDecimal maxScore;
    //权重
    private BigDecimal weight;
    //展示顺序
    private Integer sortNo;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
