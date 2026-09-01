package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.TeamFlowStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamApplicationVO {
    private Integer applicationId;
    private Integer recruitmentId;
    private Integer teamId;
    private Integer userId;
    private String userName;
    private String campus;
    private String major;
    private List<String> tags;
    private TeamFlowStatus status;
    private LocalDateTime updateTime;
}
