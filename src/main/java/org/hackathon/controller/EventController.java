package org.hackathon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.*;
import org.hackathon.data.vo.*;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * 创建赛事
     * @param dto 赛事信息
     * @return 赛事id
     */
    @PostMapping
    @Require(SUPER)
    public ResponseEntity<Result<EventIdVO>> createEvent(@RequestBody @Valid CreateEventDTO dto){
        return Result.success(new EventIdVO(eventService.createEvent(dto)), "创建成功");
    }

    /**
     * 创建赛道
     * @param dto 赛道信息
     * @return 赛道id
     */
    @PostMapping("/{eventId}/track")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<TrackIdVO>> createTrack(
            @RequestBody @Valid CreateTrackDTO dto, Context ctx){
        return Result.success(new TrackIdVO(eventService.createTrack(dto, ctx)), "创建成功");
    }

    /**
     * 获得赛事基本信息
     * @return 赛事信息及赛道名录
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<Result<EventInfoVO>> getEvent(Context ctx){
        return Result.success(eventService.getEvent(ctx), "获取成功");
    }

    /**
     * 更新赛事
     * @param dto 全量dto
     * @return ok
     */
    @PutMapping("/{eventId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updateEvent(
            @RequestBody @Valid UpdateEventDTO dto, Context ctx) {
        eventService.updateEvent(dto, ctx);
        return Result.ok();
    }

    @PutMapping("/{eventId}/team-config")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updateTeamConfig(
            @RequestBody @Valid UpdateTeamConfigDTO dto, Context ctx) {
        eventService.updateTeamConfig(dto, ctx);
        return Result.ok();
    }

    /**
     * 删除赛事
     * @return ok
     */
    @DeleteMapping("/{eventId}")
    @Require(SUPER)
    public ResponseEntity<Result<Void>> deleteEvent(Context ctx) {
        eventService.deleteEvent(ctx);
        return Result.ok();
    }

    /**
     * 查询分页赛事
     * @param param 查询参数
     * @param dto 查询条件
     * @return 分页简介
     */
    @PostMapping("/list")
    public ResponseEntity<Result<IPage<EventBriefVO>>> getEventPage(@Valid PageParamDTO param,
            @RequestBody @Valid QueryEventDTO dto){
        return Result.success(eventService.getEventPage(dto, param), "获取成功");
    }
}
