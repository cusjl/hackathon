package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EventStatus {
    PREP(0, "赛前准备"),
    REG(1, "报名阶段"),
    MID(2, "赛前休整"),
    LIVE(3, "赛事进行"),
    END(4, "赛事结束");
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;
    EventStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
