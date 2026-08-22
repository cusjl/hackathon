package org.hackathon.data.vo;

/**
 * 回避结果：原任务被置为已回避，系统重新分发后给出接手评委，无可用评委时为空
 */
public record RecuseResultVO(Integer recusedAssignmentId, Integer newAssignmentId,
                             Integer newJudgeId, String newJudgeName, String message) {
}
