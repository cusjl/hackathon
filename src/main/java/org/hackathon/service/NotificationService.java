package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.ReadNoticeDTO;
import org.hackathon.data.enums.NoticeType;
import org.hackathon.data.po.Notification;
import org.hackathon.data.vo.NotificationVO;
import org.hackathon.mapper.NotificationMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.time.format.DateTimeFormatter;

/**
 * 站内通知：评审模块向队长与评委「推送」的落地形式。
 * 写入仅追加，读取一律按接收人过滤，不存在跨用户读取的入口。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationMapper notificationMapper;

    /**
     * 推送一条通知
     * @param userId 接收人
     * @param anchors 赛事/轮次/作品锚点，便于前端跳转
     * @param refId 关联业务id，如 flag_id、assignment_id
     */
    public void push(Integer userId, NoticeType type, String title, String content,
                     Integer eventId, Integer phaseId, Integer submissionId, Integer refId) {
        Notification notification = new Notification(null, userId, type, title, content,
                eventId, phaseId, submissionId, refId, false, LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    /**
     * 向一组接收人推送同一条通知
     */
    public void pushAll(Collection<Integer> userIds, NoticeType type, String title, String content,
                        Integer eventId, Integer phaseId, Integer submissionId, Integer refId) {
        userIds.forEach(userId ->
                push(userId, type, title, content, eventId, phaseId, submissionId, refId));
    }

    /**
     * 本人通知列表，未读优先
     * @param unread 仅看未读
     */
    public IPage<NotificationVO> getNotificationPage(PageParamDTO param, Boolean unread, Integer userId) {
        Page<Notification> page = new Page<>(param.getPage(), param.getSize());
        return notificationMapper.selectPageByUser(page, userId, unread).convert(po -> {
            NotificationVO vo = new NotificationVO();
            BeanUtils.copyProperties(po, vo);
            return vo;
        });
    }

    public long countUnread(Integer userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 标记已读，不传 id 时标记本人全部通知；条件恒定带上本人 user_id，避免越权改动他人通知
     */
    public void read(ReadNoticeDTO dto, Integer userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, false)
                .set(Notification::getReadFlag, true);
        if (dto != null && dto.getNotificationIds() != null && !dto.getNotificationIds().isEmpty()) {
            wrapper.in(Notification::getNotificationId, dto.getNotificationIds());
        }
        notificationMapper.update(null, wrapper);
    }
}
