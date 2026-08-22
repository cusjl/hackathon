package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.ReviewStatus;
import org.hackathon.data.po.ReviewAssignment;

import java.util.List;

@Mapper
public interface ReviewAssignmentMapper extends BaseMapper<ReviewAssignment> {

    /**
     * 评委在某作品上仍然持有的任务（待评或已评），回避/移交后查不到
     */
    default ReviewAssignment selectHeld(Integer submissionId, Integer judgeId) {
        return selectOne(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getSubmissionId, submissionId)
                .eq(ReviewAssignment::getJudgeId, judgeId)
                .in(ReviewAssignment::getStatus, ReviewStatus.PENDING, ReviewStatus.DONE));
    }

    default boolean existsHeld(Integer submissionId, Integer judgeId) {
        return selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getSubmissionId, submissionId)
                .eq(ReviewAssignment::getJudgeId, judgeId)
                .in(ReviewAssignment::getStatus, ReviewStatus.PENDING, ReviewStatus.DONE)) > 0;
    }

    /**
     * 某作品上的全部任务（含已回避、已移交），用于排除重复分发
     */
    default List<ReviewAssignment> selectBySubmission(Integer submissionId) {
        return selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getSubmissionId, submissionId)
                .orderByAsc(ReviewAssignment::getAssignmentId));
    }

    default List<ReviewAssignment> selectBySubmissions(List<Integer> submissionIds) {
        if (submissionIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .in(ReviewAssignment::getSubmissionId, submissionIds)
                .orderByAsc(ReviewAssignment::getAssignmentId));
    }

    /**
     * 评委在某轮次下仍持有的任务，即评委工作台的待评列表数据源
     */
    default List<ReviewAssignment> selectByPhaseJudge(Integer phaseId, Integer judgeId) {
        return selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getPhaseId, phaseId)
                .eq(ReviewAssignment::getJudgeId, judgeId)
                .in(ReviewAssignment::getStatus, ReviewStatus.PENDING, ReviewStatus.DONE)
                .orderByAsc(ReviewAssignment::getAssignmentId));
    }

    /**
     * 本轮全部任务，管理员进度看板与计分的数据源
     */
    default List<ReviewAssignment> selectByPhase(Integer phaseId) {
        return selectList(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getPhaseId, phaseId)
                .orderByAsc(ReviewAssignment::getAssignmentId));
    }

    /**
     * 评委在本轮持有的任务数，重新分发时按此挑选负载最轻的评委
     */
    default long countHeldByJudge(Integer phaseId, Integer judgeId) {
        return selectCount(new LambdaQueryWrapper<ReviewAssignment>()
                .eq(ReviewAssignment::getPhaseId, phaseId)
                .eq(ReviewAssignment::getJudgeId, judgeId)
                .in(ReviewAssignment::getStatus, ReviewStatus.PENDING, ReviewStatus.DONE));
    }
}
