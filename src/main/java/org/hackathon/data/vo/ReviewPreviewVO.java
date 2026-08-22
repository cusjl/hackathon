package org.hackathon.data.vo;

import lombok.Data;

/**
 * 作品在线预览入口：文档/录屏走预签名直读地址，在线 Demo 直接给出可 iframe 嵌入的网址。
 */
@Data
public class ReviewPreviewVO {
    private Integer submissionId;
    //演示文档 PDF/PPT，内置阅读器直接加载
    private PreviewItemVO doc;
    //演示录屏，播放器直接拉流
    private PreviewItemVO video;
    //以链接形式提交的演示视频
    private String videoUrl;
    //源码压缩包，浏览器无法预览，仅提供直读地址
    private PreviewItemVO archive;
    //在线 Demo 网址，评委端 iframe 嵌入
    private String demoUrl;
    private String repoUrl;
}
