package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    @TableId(type = IdType.INPUT)
    private Integer userId;
    private String casId;
    private String campus;
    private String major;
    private String introduction;
    private String tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public List<String> getTagsAsList() {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(tags.split(","));
    }
}
