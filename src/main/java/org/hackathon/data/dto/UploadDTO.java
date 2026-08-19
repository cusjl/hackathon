package org.hackathon.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hackathon.data.enums.FileScope;

@Data
public class UploadDTO {
    @NotNull(message = "文件用途不能为空")
    private FileScope scope;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 200, message = "文件名长度不能超过 200 个字符")
    private String filename;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须为正数")
    private Long sizeBytes;
}
