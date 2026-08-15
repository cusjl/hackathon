package org.hackathon.data.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationVO {
    private Boolean registered;
    private Integer trackId;
    //null表示未组队
    private Integer teamId;
    private Integer version;
}
