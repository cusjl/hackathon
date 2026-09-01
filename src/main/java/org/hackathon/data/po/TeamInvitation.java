package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.TeamFlowStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamInvitation {
    @TableId(type = IdType.AUTO)
    private Integer invitationId;
    private Integer teamId;
    private Integer userId;
    private Integer inviterId;
    private TeamFlowStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
