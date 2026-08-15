package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TeamEnum {
    SAME_MAJOR(0, "同专业"),
    CROSS_MAJOR(1, "跨专业"),
    CROSS_CAMPUS(2, "跨校区");
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    TeamEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
