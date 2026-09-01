package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 站内通知类型，评审模块统一通过 notification 表向评委/队长推送
 */
@Getter
public enum NoticeType {
    REVIEW_ASSIGN(0, "评审任务指派"),
    REVIEW_TRANSFER(1, "评审任务移交"),
    REVIEW_URGE(2, "打分催办"),
    DEMO_FLAG(3, "作品异常与补交"),
    SUPPLEMENT_CLOSED(4, "补交窗口关闭"),
    TEAM_INVITATION(5, "组队邀请"),
    TEAM_APPLICATION(6, "入队申请"),
    TEAM_APPLICATION_RESULT(7, "入队申请结果"),
    TEAM_LEADER_CHANGED(8, "队长变更"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    NoticeType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
