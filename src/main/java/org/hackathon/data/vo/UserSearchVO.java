package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchVO {
    private Integer userId;
    private String userName;
    private Boolean studentFlag;
    private String campus;
    private String major;
    private List<String> tags;
}
