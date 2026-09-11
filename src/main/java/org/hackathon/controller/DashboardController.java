package org.hackathon.controller;

import static org.hackathon.security.Role.EVENT_ADMIN;

import com.baomidou.mybatisplus.core.metadata.IPage;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.hackathon.data.dto.*;
import org.hackathon.data.enums.DashboardDetailKind;
import org.hackathon.data.vo.Result;
import org.hackathon.security.*;
import org.hackathon.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard/event/{eventId}")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping
    public ResponseEntity<Result<Map<String, Object>>> publicDashboard(Context ctx) {
        return Result.success(service.publicDashboard(ctx), "获取成功");
    }

    @GetMapping("/admin")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Map<String, Object>>> admin(Context ctx) {
        return Result.success(service.adminDashboard(ctx), "获取成功");
    }

    @GetMapping("/config")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Map<String, Object>>> config(Context ctx) {
        return Result.success(service.config(ctx), "获取成功");
    }

    @PutMapping("/config")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Map<String, Object>>> saveConfig(
            @Valid @RequestBody PublicMetricsDTO dto, Context ctx) {
        return Result.success(service.saveConfig(dto, ctx), "保存成功");
    }

    @GetMapping("/admin/details")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<IPage<Map<String, Object>>>> details(
            @RequestParam DashboardDetailKind kind,
            @RequestParam(required = false) Integer phaseId,
            @Valid PageParamDTO page,
            Context ctx) {
        return Result.success(service.details(kind, phaseId, page, ctx), "获取成功");
    }
}
