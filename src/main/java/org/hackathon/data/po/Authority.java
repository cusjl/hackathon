package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.AuthorityEnum;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Authority {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private AuthorityEnum type;
    private Integer eventId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Integer getTypeValue() {
        return type.getValue();
    }
}
