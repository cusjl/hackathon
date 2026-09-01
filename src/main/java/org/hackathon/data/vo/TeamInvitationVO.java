package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.TeamFlowStatus;

import java.time.LocalDateTime;

@Data
public class TeamInvitationVO {
    private Integer invitationId;
    private Integer teamId;
    private String teamName;
    private Integer eventId;
    private String eventName;
    private Integer trackId;
    private String trackName;
    private Integer inviterId;
    private String inviterName;
    private TeamFlowStatus status;
    private LocalDateTime updateTime;
}
