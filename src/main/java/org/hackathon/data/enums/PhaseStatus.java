package org.hackathon.data.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PhaseStatus {
    PREP("未开始"), SUBMIT("正在提交"), MID("提交结束"), REVIEW("正在评审"), END("已结束");
    @JsonValue
    private final String desc;
    PhaseStatus(String desc) {
        this.desc = desc;
    }
}
