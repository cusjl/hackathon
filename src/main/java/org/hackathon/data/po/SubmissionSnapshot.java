package org.hackathon.data.po;

import lombok.Data;

/**
 * 作品内容快照，字段与 {@link SubmissionConfig} 中的提交项一一对应。
 * 既用于版本日志的 json 存档，也用于历史版本回显。
 */
@Data
public class SubmissionSnapshot {
    //代码仓库地址
    private String repoUrl;
    //开源许可协议
    private String licenseType;
    //二次开发原项目说明
    private String derivedFrom;
    //源码压缩包
    private Long archiveFileId;
    //演示视频（文件）
    private Long videoFileId;
    //演示视频（链接）
    private String videoUrl;
    //演示文档 PPT/PDF
    private Long docFileId;
    //在线 Demo 网址
    private String demoUrl;
    //项目详细介绍 Markdown
    private String introMd;
    //开源及 AI 声明
    private String declaration;
}
