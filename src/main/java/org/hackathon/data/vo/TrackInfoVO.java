package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackInfoVO {
    private Integer trackId;
    private String name;
    private String descMd;
    private List<PhaseBriefVO> phases;
    private Integer version;
    private Integer eventId;
    private String eventName;
}
