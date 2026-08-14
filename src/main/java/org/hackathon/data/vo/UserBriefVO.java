package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBriefVO {
    private Integer userId;
    private String name;
    private Boolean studentFlag;
    private String phone;
    private String email;
}
