package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 评审任务状态。回避与移交都属于终态，任务由新的一条 assignment 继续承接。
 */
@Getter
public enum ReviewStatus {
    PENDING(0, "待评审"),
    DONE(1, "已完成"),
    RECUSED(2, "已回避"),
    TRANSFERRED(3, "已移交"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    ReviewStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 任务是否仍由该评委持有（待评或已评），回避/移交后不再持有
     */
    public boolean isHeld() {
        return this == PENDING || this == DONE;
    }
}
