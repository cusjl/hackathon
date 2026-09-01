package org.hackathon.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationVO {
    private Integer recommendationId;
    private Integer eventId;
    private Integer trackId;
    private String trackName;
    private Integer userId;
    private String userName;
    private String campus;
    private String major;
    private List<String> profileTags;
    private String introduction;
    private List<String> skills;
    private Integer version;
    private LocalDateTime updateTime;
}
