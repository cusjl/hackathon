package org.hackathon.data.dto;

import lombok.Data;
import org.hackathon.data.enums.TeamStatus;

@Data
public class QueryTeamDTO {
    private Integer trackId;
    private String name;
    private TeamStatus status;
}
