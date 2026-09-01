package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TeamFlowStatus {
    PENDING(0, "待处理"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已取消");

    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    TeamFlowStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
