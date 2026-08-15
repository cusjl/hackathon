package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.enums.TeamStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Team {
    @TableId(type = IdType.AUTO)
    private Integer teamId;
    private String name;
    private Integer eventId;
    private Integer trackId;
    private Integer leaderId;
    private Integer size;
    private String introduction;
    private TeamEnum type;
    private TeamStatus status;
    private Integer currentPhaseId;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
