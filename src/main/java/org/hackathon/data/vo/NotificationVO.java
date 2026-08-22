package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.NoticeType;

import java.time.LocalDateTime;

/**
 * 站内通知
 */
@Data
public class NotificationVO {
    private Long notificationId;
    private NoticeType type;
    private String title;
    private String content;
    private Integer eventId;
    private Integer phaseId;
    private Integer submissionId;
    private Integer refId;
    private Boolean readFlag;
    private LocalDateTime createTime;
}
