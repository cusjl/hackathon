package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.Registration;

import java.util.List;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    default boolean existsTeamMember(Integer userId, Integer teamId) {
        return selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getTeamId, teamId)) > 0;
    }

    /**
     * 队伍全部成员id，评审侧用于利益回避判定与队伍信息展示
     */
    default List<Integer> selectMemberIds(Integer teamId) {
        return selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getTeamId, teamId)
                .select(Registration::getUserId))
                .stream().map(Registration::getUserId).toList();
    }
}
