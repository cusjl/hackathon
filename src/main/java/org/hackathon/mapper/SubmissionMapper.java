package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.SubmissionStatus;
import org.hackathon.data.po.Submission;

import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    default Submission selectByTeamPhase(Integer teamId, Integer phaseId) {
        return selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getTeamId, teamId)
                .eq(Submission::getPhaseId, phaseId));
    }

    /**
     * 本轮全部作品，评委指派与计分排名的数据源
     */
    default List<Submission> selectByPhase(Integer phaseId) {
        return selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getPhaseId, phaseId)
                .orderByAsc(Submission::getSubmissionId));
    }

    /**
     * 查询所属轮次已过提交截止、但状态尚未锁定的作品
     */
    default List<Submission> selectExpiredSubmissions() {
        return selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, SubmissionStatus.SUBMITTED)
                .inSql(Submission::getPhaseId, "SELECT phase_id FROM phase WHERE submit_end < NOW()"));
    }
}
