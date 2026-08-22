package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.Vote;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface VoteMapper extends BaseMapper<Vote> {

    default long countToday(Integer userId, Integer phaseId, LocalDate date) {
        return selectCount(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getUserId, userId)
                .eq(Vote::getPhaseId, phaseId)
                .eq(Vote::getVoteDate, date));
    }

    default boolean existsByUserAndSubmission(Integer userId, Integer submissionId) {
        return selectCount(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getUserId, userId)
                .eq(Vote::getSubmissionId, submissionId)) > 0;
    }

    default List<Vote> selectByPhase(Integer phaseId) {
        return selectList(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getPhaseId, phaseId));
    }

    default long countBySubmission(Integer submissionId) {
        return selectCount(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getSubmissionId, submissionId));
    }
}
