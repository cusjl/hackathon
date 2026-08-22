package org.hackathon.data.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户在本轮的投票状态。
 */
@Data
public class VoteStatusVO {
    private Integer phaseId;
    private Boolean poll;
    //每日票数上限，未开启投票时为 0
    private Integer dailyCap;
    private Integer todayUsed;
    private Integer todayRemaining;
    //本轮已投过的作品列表（含往日所投）
    private List<Integer> votedSubmissionIds;
}
