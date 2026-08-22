package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 评审任务的来源，用于追溯一条任务是人工指派还是系统重新分发产生的
 */
@Getter
public enum AssignSource {
    MANUAL(0, "管理员指派"),
    REDISTRIBUTE(1, "回避后重新分发"),
    TRANSFER(2, "逾期移交"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    AssignSource(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
