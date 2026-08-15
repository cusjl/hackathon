package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TeamStatus {
    ACTIVE(0, "晋级"),
    FAILED(1, "淘汰"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    TeamStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
