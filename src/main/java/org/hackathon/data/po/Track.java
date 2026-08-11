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
public class Track {
    @TableId(type = IdType.AUTO)
    private Integer trackId;
    private Integer eventId;
    private String name;
    private String descMd;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
