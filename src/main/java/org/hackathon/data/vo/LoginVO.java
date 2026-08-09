package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String name;
    private Boolean isStudent;
    private String casId;
}
