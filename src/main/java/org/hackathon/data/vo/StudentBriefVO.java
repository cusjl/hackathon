package org.hackathon.data.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentBriefVO {
    private String campus;
    private String major;
    private String introduction;
    private List<String> tags;
}
