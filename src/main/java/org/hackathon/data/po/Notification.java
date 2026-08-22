package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.NoticeType;

import java.time.LocalDateTime;

/**
 * 站内通知，评审模块的「推送」落地形式：仅追加，读取方按接收人过滤。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long notificationId;
    //接收人
    private Integer userId;
    private NoticeType type;
    private String title;
    private String content;
    private Integer eventId;
    private Integer phaseId;
    private Integer submissionId;
    //关联业务id，如 flag_id / assignment_id
    private Integer refId;
    private Boolean readFlag;
    private LocalDateTime createTime;
}
