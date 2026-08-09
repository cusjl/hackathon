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
public class Student {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private String name;
    private String campus;
    private String major;
    private String introduction;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
