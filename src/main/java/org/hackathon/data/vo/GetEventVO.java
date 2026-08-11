package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GetEventVO {
    private Integer eventId;
    private String name;
    private EventStatus status;
    private LocalDateTime regBeg;
    private LocalDateTime regEnd;
    private LocalDateTime liveBeg;
    private LocalDateTime liveEnd;
    private String introduction;
    private String tags;
    private String notice;
    private List<TrackNameVO> tracks;
}
