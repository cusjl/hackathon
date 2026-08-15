package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberVO {
    private Integer userId;
    private String name;
    private String casId;
    private String campus;
    private String major;
    private List<String> tags;
    private Boolean leader;
}
