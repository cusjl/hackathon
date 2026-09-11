package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;

import lombok.Data;

import org.hackathon.data.enums.PublicMetric;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(autoResultMap = true)
public class DashboardConfig {
    @TableId(type = IdType.INPUT)
    private Integer eventId;

    @TableField(typeHandler = Jackson3TypeHandler.class)
    private List<PublicMetric> publicMetrics;

    @Version private Integer version;
    private LocalDateTime updateTime;
}
