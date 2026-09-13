package org.hackathon.controller;

import static org.hackathon.security.Role.SUPER;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.hackathon.data.dto.PublicMetricsDTO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.Require;
import org.hackathon.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class GlobalDashboardController {
    private final DashboardService service;

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> publicDashboard() {
        return Result.success(service.globalPublicDashboard(), "获取成功");
    }

    @GetMapping("/config")
    @Require(SUPER)
    public ResponseEntity<Result<Map<String, Object>>> config() {
        return Result.success(service.globalConfig(), "获取成功");
    }

    @PutMapping("/config")
    @Require(SUPER)
    public ResponseEntity<Result<Map<String, Object>>> saveConfig(
            @Valid @RequestBody PublicMetricsDTO dto) {
        return Result.success(service.saveGlobalConfig(dto), "保存成功");
    }
}
