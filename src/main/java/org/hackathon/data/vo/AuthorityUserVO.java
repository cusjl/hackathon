package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.AuthorityEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorityUserVO {
    private Integer userId;
    private String name;
    private AuthorityEnum authority;
}
