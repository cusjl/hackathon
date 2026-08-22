package org.hackathon.data.vo;

import java.util.List;

/**
 * 指派结果：created 新建任务数，skipped 因已存在或利益冲突跳过的组合
 */
public record AssignResultVO(Integer created, Integer skipped, List<String> skippedReasons) {
}
