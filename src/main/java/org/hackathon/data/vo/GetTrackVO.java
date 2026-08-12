package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetTrackVO {
    private Integer trackId;
    private String name;
    private String descMd;
    private Integer version;
    private List<BriefPhaseVO> phases;
}
