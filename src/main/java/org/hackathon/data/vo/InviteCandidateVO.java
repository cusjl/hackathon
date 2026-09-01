package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InviteCandidateVO {
    private Integer userId;
    private String userName;
    private String campus;
    private String major;
    private List<String> tags;
}
