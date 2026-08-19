package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.FileScope;
import org.hackathon.data.enums.FileStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileObject {
    @TableId(type = IdType.AUTO)
    private Long fileId;
    private String objectKey;
    private FileScope scope;
    private FileStatus status;
    private String originName;
    private String contentType;
    private Long sizeBytes;
    private String etag;
    private Integer uploaderId;
    private Integer userId;
    private Integer eventId;
    private Integer trackId;
    private Integer phaseId;
    private Integer teamId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Map<String, Integer> anchors() {
        Map<String, Integer> ids = new HashMap<>();
        ids.put("userId", userId);
        ids.put("eventId", eventId);
        ids.put("trackId", trackId);
        ids.put("phaseId", phaseId);
        ids.put("teamId", teamId);
        ids.values().removeIf(Objects::isNull);
        return ids;
    }

    public String extension() {
        int dot = originName.lastIndexOf('.');
        return dot < 0 ? "" : originName.substring(dot + 1).toLowerCase();
    }
}
