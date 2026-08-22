package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.FlagStatus;
import org.hackathon.data.enums.FlagTarget;
import org.hackathon.data.po.ReviewFlag;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewFlagMapper extends BaseMapper<ReviewFlag> {

    default List<ReviewFlag> selectBySubmission(Integer submissionId) {
        return selectList(new LambdaQueryWrapper<ReviewFlag>()
                .eq(ReviewFlag::getSubmissionId, submissionId)
                .orderByAsc(ReviewFlag::getFlagId));
    }

    default List<ReviewFlag> selectBySubmissions(List<Integer> submissionIds) {
        if (submissionIds.isEmpty()) return List.of();
        return selectList(new LambdaQueryWrapper<ReviewFlag>()
                .in(ReviewFlag::getSubmissionId, submissionIds)
                .orderByAsc(ReviewFlag::getFlagId));
    }

    /**
     * 同一提交项上未处理的标记，避免重复开窗
     */
    default ReviewFlag selectOpenByTarget(Integer submissionId, FlagTarget target) {
        return selectOne(new LambdaQueryWrapper<ReviewFlag>()
                .eq(ReviewFlag::getSubmissionId, submissionId)
                .eq(ReviewFlag::getTarget, target)
                .eq(ReviewFlag::getStatus, FlagStatus.OPEN)
                .last("LIMIT 1"));
    }

    /**
     * 作品当前仍然有效的补交窗口，无有效窗口时返回空表
     */
    default List<ReviewFlag> selectOpenWindows(Integer submissionId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapper<ReviewFlag>()
                .eq(ReviewFlag::getSubmissionId, submissionId)
                .eq(ReviewFlag::getStatus, FlagStatus.OPEN)
                .gt(ReviewFlag::getSupplementEnd, now)
                .orderByAsc(ReviewFlag::getFlagId));
    }

    /**
     * 窗口已过期但仍处于待补交状态的标记，由定时任务关窗
     */
    default List<ReviewFlag> selectExpiredWindows(LocalDateTime now) {
        return selectList(new LambdaQueryWrapper<ReviewFlag>()
                .eq(ReviewFlag::getStatus, FlagStatus.OPEN)
                .le(ReviewFlag::getSupplementEnd, now));
    }
}
