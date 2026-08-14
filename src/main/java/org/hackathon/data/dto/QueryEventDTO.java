package org.hackathon.data.dto;

import lombok.Data;
import org.hackathon.data.enums.EventStatus;

import java.util.List;

@Data
public class QueryEventDTO {
    private String name;
    private List<EventStatus> statuses;
    private List<String> tags;
}
