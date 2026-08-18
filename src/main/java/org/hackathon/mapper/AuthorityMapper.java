package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.po.Authority;

@Mapper
public interface AuthorityMapper extends BaseMapper<Authority> {

    default boolean existsSuper(Integer userId) {
        return selectCount(new LambdaQueryWrapper<Authority>()
                .eq(Authority::getUserId, userId)
                .eq(Authority::getType, AuthorityEnum.SUPER)) > 0;
    }

    default boolean existsEventAdmin(Integer userId, Integer eventId) {
        return selectCount(new LambdaQueryWrapper<Authority>()
                .eq(Authority::getUserId, userId)
                .and(w -> w.eq(Authority::getType, AuthorityEnum.SUPER)
                        .or(x -> x.eq(Authority::getType, AuthorityEnum.ADMIN)
                                .eq(Authority::getEventId, eventId)))) > 0;
    }

    default boolean existsEventJudge(Integer userId, Integer eventId) {
        return selectCount(new LambdaQueryWrapper<Authority>()
                .eq(Authority::getUserId, userId)
                .eq(Authority::getType, AuthorityEnum.JUDGE)
                .eq(Authority::getEventId, eventId)) > 0;
    }
}
