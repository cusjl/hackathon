package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.po.ReviewDimension;

import java.math.BigDecimal;

/**
 * 轮次评分维度，评委工作台据此渲染打分表
 */
@Data
public class DimensionVO {
    private Integer dimensionId;
    private String name;
    private String description;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private Integer sortNo;
    private Integer version;

    public static DimensionVO of(ReviewDimension po) {
        DimensionVO vo = new DimensionVO();
        vo.setDimensionId(po.getDimensionId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        vo.setMaxScore(po.getMaxScore());
        vo.setWeight(po.getWeight());
        vo.setSortNo(po.getSortNo());
        vo.setVersion(po.getVersion());
        return vo;
    }
}
