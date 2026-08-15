package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.TeamEnum;

import java.util.List;

@Data
public class TeamInfoVO {
    private Integer teamId;
    private String name;
    private Integer eventId;
    private String eventName;
    private Integer trackId;
    private String trackName;
    private Integer size;
    private String introduction;
    private TeamEnum type;
    private Integer version;
    private List<TeamMemberVO> members;
}
