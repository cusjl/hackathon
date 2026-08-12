package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateStudentVO {
    private String token;
    private String name;
    private Boolean studentFlag;
    private String casId;
    private Boolean existed;
    private List<AuthorityVO> authorities;
}
