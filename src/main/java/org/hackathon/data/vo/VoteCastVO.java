package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteCastVO {
    private Integer submissionId;
    //该作品当前总票数
    private Long totalVotes;
    //今日已投票数（含本票）
    private Integer todayUsed;
    private Integer todayRemaining;
}
