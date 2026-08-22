package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.po.Authority;

import java.util.List;

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

    /**
     * 赛事下的全部评委id，评委指派与重新分发的候选集
     */
    default List<Integer> selectEventJudgeIds(Integer eventId) {
        return selectUserIds(AuthorityEnum.JUDGE, eventId);
    }

    /**
     * 赛事下的全部赛管id，用于分发失败等情况的兜底通知
     */
    default List<Integer> selectEventAdminIds(Integer eventId) {
        return selectUserIds(AuthorityEnum.ADMIN, eventId);
    }

    private List<Integer> selectUserIds(AuthorityEnum type, Integer eventId) {
        return selectList(new LambdaQueryWrapper<Authority>()
                .eq(Authority::getType, type)
                .eq(Authority::getEventId, eventId)
                .orderByAsc(Authority::getUserId))
                .stream().map(Authority::getUserId).toList();
    }
}
