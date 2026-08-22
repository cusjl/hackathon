package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.ReviewScore;

import java.util.List;

@Mapper
public interface ReviewScoreMapper extends BaseMapper<ReviewScore> {

    default List<ReviewScore> selectByAssignment(Integer assignmentId) {
        return selectList(new LambdaQueryWrapper<ReviewScore>()
                .eq(ReviewScore::getAssignmentId, assignmentId));
    }

    default List<ReviewScore> selectByAssignments(List<Integer> assignmentIds) {
        if (assignmentIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<ReviewScore>()
                .in(ReviewScore::getAssignmentId, assignmentIds));
    }

    default void deleteByAssignment(Integer assignmentId) {
        delete(new LambdaQueryWrapper<ReviewScore>()
                .eq(ReviewScore::getAssignmentId, assignmentId));
    }

    /**
     * 本轮是否已有任何维度打分，用于锁定维度配置
     */
    default boolean existsByPhase(Integer phaseId) {
        return selectCount(new LambdaQueryWrapper<ReviewScore>()
                .inSql(ReviewScore::getAssignmentId,
                        "SELECT assignment_id FROM review_assignment WHERE phase_id = " + phaseId)) > 0;
    }
}
