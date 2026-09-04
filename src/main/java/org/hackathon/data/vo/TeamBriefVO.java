package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.enums.TeamJoinUnavailableReason;
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
    private Integer maxSize;
    private TeamEnum type;
    private TeamStatus status;
    /** Only populated by the student-facing joinable-team search. */
    private Boolean joinable;
    /** Null when joinable is true; otherwise identifies the conflicting profile field. */
    private TeamJoinUnavailableReason unavailableReason;
    private LocalDateTime updateTime;
}
