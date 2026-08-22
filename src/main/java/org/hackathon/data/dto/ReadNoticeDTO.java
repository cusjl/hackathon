package org.hackathon.data.dto;

import lombok.Data;

import java.util.List;

/**
 * 标记通知已读，不传 id 时标记本人全部通知
 */
@Data
public class ReadNoticeDTO {
    private List<Long> notificationIds;
}
