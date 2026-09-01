package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
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
    private LocalDateTime regBeg;
    private LocalDateTime regEnd;
    private LocalDateTime liveBeg;
    private LocalDateTime liveEnd;
    private String introduction;
    private String tags;
    private String notice;
    private Integer teamMinSize;
    private Integer teamMaxSize;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @SuppressWarnings("unused")
    public EventStatus getStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(regBeg)) {
            return EventStatus.PREP;
        } else if (now.isBefore(regEnd)) {
            return EventStatus.REG;
        } else if (now.isBefore(liveBeg)) {
            return EventStatus.MID;
        } else if (now.isBefore(liveEnd)) {
            return EventStatus.LIVE;
        }
        return EventStatus.END;
    }

    @SuppressWarnings("unused")
    public void setStatus(EventStatus status) {
    }
}
