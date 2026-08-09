package org.hackathon.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateJwtDTO {
    private Integer id;
    private String name;
    private Boolean isStudent;
    private String casID;
}
