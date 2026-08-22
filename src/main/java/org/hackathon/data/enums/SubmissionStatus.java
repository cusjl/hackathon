package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SubmissionStatus {
    SUBMITTED(0, "已提交"),
    LOCKED(1, "已提交，待评审"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    SubmissionStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
