package org.hackathon.controller;

import static org.hackathon.security.Role.*;

import com.baomidou.mybatisplus.core.metadata.IPage;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.hackathon.data.dto.*;
import org.hackathon.data.enums.ShowcaseStatus;
import org.hackathon.data.vo.Result;
import org.hackathon.security.*;
import org.hackathon.service.ShowcaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/showcase")
@RequiredArgsConstructor
public class ShowcaseController {
    private final ShowcaseService service;

    @GetMapping("/list")
    public ResponseEntity<Result<IPage<Map<String, Object>>>> list(
            @RequestParam(required = false) Integer eventId,
            @RequestParam(required = false) Integer trackId,
            @RequestParam(required = false) String keyword,
            @Valid PageParamDTO page) {
        return Result.success(service.list(eventId, trackId, keyword, null, page, false), "获取成功");
    }

    @GetMapping("/{showcaseId}")
    public ResponseEntity<Result<Map<String, Object>>> detail(@PathVariable Integer showcaseId) {
        return Result.success(service.publicDetail(showcaseId), "获取成功");
    }

    @GetMapping("/team/{teamId}")
    @Require({TEAM_MEMBER, EVENT_ADMIN})
    public ResponseEntity<Result<Map<String, Object>>> manage(Context ctx) {
        return Result.success(service.manage(ctx), "获取成功");
    }

    @PutMapping("/team/{teamId}")
    @Require(TEAM_LEADER)
    public ResponseEntity<Result<Map<String, Object>>> save(
            @Valid @RequestBody SaveShowcaseDTO dto, Context ctx) {
        return Result.success(service.save(dto, ctx), "保存成功");
    }

    @PostMapping("/team/{teamId}/publish")
    @Require(TEAM_LEADER)
    public ResponseEntity<Result<Map<String, Object>>> publish(
            @Valid @RequestBody ShowcaseActionDTO dto, Context ctx) {
        return Result.success(service.publish(dto, ctx), "发布成功");
    }

    @PostMapping("/team/{teamId}/withdraw")
    @Require(TEAM_LEADER)
    public ResponseEntity<Result<Map<String, Object>>> withdraw(
            @Valid @RequestBody ShowcaseActionDTO dto, Context ctx) {
        return Result.success(service.withdraw(dto, ctx), "撤回成功");
    }

    @GetMapping("/event/{eventId}/manage/list")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<IPage<Map<String, Object>>>> managementList(
            @RequestParam(required = false) Integer trackId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ShowcaseStatus status,
            @Valid PageParamDTO page,
            Context ctx) {
        return Result.success(
                service.list(ctx.event().getEventId(), trackId, keyword, status, page, true),
                "获取成功");
    }

    @PutMapping("/event/{eventId}/{showcaseId}/moderation")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Map<String, Object>>> moderate(
            @PathVariable Integer showcaseId,
            @Valid @RequestBody ModerateShowcaseDTO dto,
            Context ctx) {
        return Result.success(service.moderate(showcaseId, dto, ctx), "操作成功");
    }
}
