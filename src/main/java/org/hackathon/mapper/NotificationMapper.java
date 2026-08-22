package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.Notification;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 本人通知，未读优先、时间倒序
     */
    default IPage<Notification> selectPageByUser(IPage<Notification> page, Integer userId, Boolean unread) {
        return selectPage(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Boolean.TRUE.equals(unread), Notification::getReadFlag, false)
                .orderByAsc(Notification::getReadFlag)
                .orderByDesc(Notification::getNotificationId));
    }

    default long countUnread(Integer userId) {
        return selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, false));
    }
}
