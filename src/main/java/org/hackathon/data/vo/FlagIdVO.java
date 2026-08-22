package org.hackathon.data.vo;

import java.time.LocalDateTime;

/**
 * 异常标记结果，同时回传本次开出的补交窗口截止时间
 */
public record FlagIdVO(Integer flagId, LocalDateTime supplementEnd) {
}
