package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AuthorityEnum {
    SUPER(0, "超级管理员"),
    ADMIN(1, "赛事管理员"),
    JUDGE(2, "赛事评委");

    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;
    AuthorityEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
