package org.hackathon.data.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.hackathon.data.po.SubmissionConfig;

/**
 * Demo 异常标记的对象，与提交项一一对应
 */
@Getter
public enum FlagTarget {
    DEMO_URL(0, "在线Demo网址"),
    VIDEO(1, "演示视频"),
    DOC(2, "演示文档"),
    ARCHIVE(3, "源码压缩包"),
    REPO(4, "代码仓库地址"),
    OTHER(5, "其他"),
    ;
    @EnumValue
    private final Integer value;
    @JsonValue
    private final String desc;

    FlagTarget(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 本轮是否开启了该提交项，未开启的提交项不允许被标记异常
     */
    public boolean enabledIn(SubmissionConfig config) {
        if (config == null) return this == OTHER;
        return switch (this) {
            case DEMO_URL -> Boolean.TRUE.equals(config.getWebsite());
            case VIDEO -> Boolean.TRUE.equals(config.getVideo());
            case DOC -> Boolean.TRUE.equals(config.getPowerpoint());
            case ARCHIVE -> Boolean.TRUE.equals(config.getZip());
            case REPO -> Boolean.TRUE.equals(config.getRepository());
            case OTHER -> true;
        };
    }
}
