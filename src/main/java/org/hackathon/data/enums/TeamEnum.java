package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum TeamEnum {
    /** 队员的专业和校区均须一致。 */
    SAME_MAJOR(0, "同专业"),
    /** 队员的专业可以不同，但校区须一致。 */
    CROSS_MAJOR(1, "跨专业"),
    /** 队员的专业和校区都可以不同。 */
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
