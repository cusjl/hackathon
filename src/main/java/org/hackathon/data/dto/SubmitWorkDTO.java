package org.hackathon.data.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交/重新提交作品。
 * 各字段是否必填由本轮 phase.submission_config 勾选项决定，服务端动态校验。
 */
@Data
public class SubmitWorkDTO {
    @Size(max = 255, message = "代码仓库地址长度不能超过255个字符")
    private String repoUrl;
    @Size(max = 50, message = "开源许可协议长度不能超过50个字符")
    private String licenseType;
    @Size(max = 500, message = "二次开发原项目说明长度不能超过500个字符")
    private String derivedFrom;
    @Positive(message = "源码压缩包文件id必须为正数")
    private Long archiveFileId;
    @Positive(message = "演示视频文件id必须为正数")
    private Long videoFileId;
    @Size(max = 500, message = "演示视频链接长度不能超过500个字符")
    private String videoUrl;
    @Positive(message = "演示文档文件id必须为正数")
    private Long docFileId;
    @Size(max = 500, message = "在线Demo网址长度不能超过500个字符")
    private String demoUrl;
    private String introMd;
    private String declaration;
    @Size(max = 200, message = "提交说明长度不能超过200个字符")
    private String changeLog;
    //乐观锁版本号，首次提交可不传
    private Integer version;
}
