package org.hackathon.data.vo;

import java.time.LocalDateTime;

public record FileUrlVO(String url, LocalDateTime expiresAt) {
}
