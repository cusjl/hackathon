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
public class TeamApplication {
    @TableId(type = IdType.AUTO)
    private Integer applicationId;
    private Integer recruitmentId;
    private Integer teamId;
    private Integer userId;
    private TeamFlowStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
