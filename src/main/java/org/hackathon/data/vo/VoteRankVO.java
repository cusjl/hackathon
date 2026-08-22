package org.hackathon.data.vo;

import lombok.Data;

/**
 * 最佳人气作品榜条目，按票数降序排列。
 */
@Data
public class VoteRankVO {
    private Integer rank;
    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    private Long voteCount;
    //当前登录用户是否已投该作品，匿名访问时为 null
    private Boolean voted;
}
