package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作品提交版本日志，每次提交追加一条，保留完整快照，仅追加不修改。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(autoResultMap = true)
public class SubmissionVersion {
    @TableId(type = IdType.AUTO)
    private Long versionId;
    private Integer submissionId;
    //版本序号，v1、v2 …
    private Integer versionNo;
    @TableField(typeHandler = Jackson3TypeHandler.class)
    private SubmissionSnapshot snapshot;
    //本次提交说明
    private String changeLog;
    private Integer submitterId;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
