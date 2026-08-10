package org.hackathon.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocalJwt {
    private Integer userId;
    private String name;
    private Boolean isStudent;
    private String casId;
}
