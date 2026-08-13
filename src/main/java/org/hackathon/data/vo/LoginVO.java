package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String name;
    private Boolean studentFlag;
    private String casId;
    private List<AuthorityEventVO> authorities;
}
