package org.hackathon.data.vo;

import lombok.Data;

import java.util.List;

/**
 * 最佳人气作品榜条目，按票数降序排列。
 */
@Data
public class VoteRankVO {
    private Integer rank;
    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    //作品介绍，Markdown 格式
    private String introMd;
    //作品技术栈
    private List<String> techStacks;
    //作品演示地址
    private String demoUrl;
    //作品视频外链
    private String videoUrl;
    private Long voteCount;
    //当前登录用户是否已投该作品，匿名访问时为 null
    private Boolean voted;
}
