package org.hackathon.data.vo;

import lombok.Data;
import org.hackathon.data.enums.AuthorityEnum;

@Data
public class AuthorityEventVO {
    private AuthorityEnum type;
    private Integer eventId;
    private String eventName;
}
