package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.Registration;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    default boolean existsTeamMember(Integer userId, Integer teamId) {
        return selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getTeamId, teamId)) > 0;
    }
}
