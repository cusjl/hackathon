package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.EventStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @TableId(type = IdType.AUTO)
    private Integer eventId;
    private String name;
    private EventStatus status;
    private LocalDateTime regBeg;
    private LocalDateTime regEnd;
    private LocalDateTime liveBeg;
    private LocalDateTime liveEnd;
    private String introduction;
    private String tags;
    private String notice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
