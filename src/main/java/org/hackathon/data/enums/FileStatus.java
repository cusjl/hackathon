package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum FileStatus {
    PENDING(0),
    READY(1),
    DELETED(2);

    @EnumValue
    private final Integer value;

    FileStatus(Integer value) {
        this.value = value;
    }
}
