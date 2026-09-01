package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.enums.TeamStatus;

import java.time.LocalDateTime;

@Data
public class TeamBriefVO {
    private Integer teamId;
    private String name;
    private Integer trackId;
    private String trackName;
    private Integer leaderId;
    private String leaderName;
    private Integer size;
    private TeamEnum type;
    private TeamStatus status;
    private LocalDateTime updateTime;
}
