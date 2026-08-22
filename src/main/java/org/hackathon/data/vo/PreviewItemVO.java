package org.hackathon.data.vo;

import java.time.LocalDateTime;

/**
 * 单个可在线预览的文件：url 为带时效的预签名直读地址，内联展示不触发下载
 */
public record PreviewItemVO(Long fileId, String name, String contentType, Long sizeBytes,
                            String url, LocalDateTime expiresAt) {
}
