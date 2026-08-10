package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum EventStatus {
    PREP(0, "赛前准备"),
    REG(1, "报名阶段"),
    MID(2, "赛前休整"),
    LIVE(3, "赛事进行"),
    OFF(4, "赛事结束");
    @EnumValue
    private final Integer value;
    private final String desc;
    EventStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
