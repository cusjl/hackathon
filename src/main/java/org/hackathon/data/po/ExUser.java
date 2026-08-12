package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExUser {
    @TableId(type = IdType.INPUT)
    private Integer userId;
    private Boolean onCampus;
    private String organization;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
