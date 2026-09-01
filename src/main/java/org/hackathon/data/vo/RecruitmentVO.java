package org.hackathon.data.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecruitmentVO {
    private Integer recruitmentId;
    private Integer teamId;
    private String teamName;
    private Integer eventId;
    private Integer trackId;
    private String trackName;
    private Integer currentSize;
    private Integer maxSize;
    private String title;
    private String description;
    private List<String> requiredTags;
    private Integer vacancies;
    private Boolean open;
    private Integer version;
    private LocalDateTime updateTime;
}
