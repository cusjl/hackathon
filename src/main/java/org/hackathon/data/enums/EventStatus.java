package org.hackathon.data.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EventStatus {
    PREP( "赛前准备"),
    REG("报名阶段"),
    MID("赛前休整"),
    LIVE("赛事进行"),
    END("赛事结束");
    @JsonValue
    private final String desc;
    EventStatus(String desc) {
        this.desc = desc;
    }
}
