package org.hackathon.data.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventDTO {
    @Pattern(regexp = "^(?!\\s*$).+", message = "名称不能为空")
    private String name;
    private LocalDateTime regBeg;
    private LocalDateTime regEnd;
    private LocalDateTime liveBeg;
    private LocalDateTime liveEnd;
    private String introduction;
    private String tags;
}
