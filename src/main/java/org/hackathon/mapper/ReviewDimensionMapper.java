package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.ReviewDimension;

import java.util.List;

@Mapper
public interface ReviewDimensionMapper extends BaseMapper<ReviewDimension> {

    /**
     * 本轮全部评分维度，按展示顺序、id 升序
     */
    default List<ReviewDimension> selectByPhase(Integer phaseId) {
        return selectList(new LambdaQueryWrapper<ReviewDimension>()
                .eq(ReviewDimension::getPhaseId, phaseId)
                .orderByAsc(ReviewDimension::getSortNo)
                .orderByAsc(ReviewDimension::getDimensionId));
    }
}
