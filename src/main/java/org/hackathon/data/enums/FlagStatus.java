package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 异常标记状态，OPEN 期间队长持有一个受限的补交窗口
 */
@Getter
public enum FlagStatus {
    OPEN(0, "待补交"),
    RESOLVED(1, "已补交"),
    CLOSED(2, "已关闭"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    FlagStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
