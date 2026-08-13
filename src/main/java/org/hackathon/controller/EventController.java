package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.CreateEventDTO;
import org.hackathon.data.dto.CreateTrackDTO;
import org.hackathon.data.dto.UpdateEventDTO;
import org.hackathon.data.vo.EventIdVO;
import org.hackathon.data.vo.TrackIdVO;
import org.hackathon.data.vo.EventInfoVO;
import org.hackathon.data.vo.Result;
import org.hackathon.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @Auth(onlySuper = true)
    public ResponseEntity<Result<EventIdVO>> createEvent(@RequestBody @Valid CreateEventDTO dto){
        return Result.success(new EventIdVO(eventService.createEvent(dto)), "创建成功");
    }

    /**
     * 创建赛道
     * @param eventId 赛事id
     * @param dto 赛道信息
     * @return 赛道id
     */
    @PostMapping("/{eventId}/track")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<TrackIdVO>> createTrack(@PathVariable Integer eventId,
            @RequestBody @Valid CreateTrackDTO dto, HttpServletRequest request){
        EventContext context = (EventContext) request.getAttribute("context");
        return Result.success(new TrackIdVO(eventService.createTrack(dto, context)), "创建成功");
    }

    /**
     * 获得赛事基本信息
     * @param eventId 赛事id
     * @return 赛事信息及赛道名录
     */
    @GetMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    public ResponseEntity<Result<EventInfoVO>> getEvent(@PathVariable Integer eventId,
            HttpServletRequest request){
        EventContext context = (EventContext) request.getAttribute("context");
        return Result.success(eventService.getEvent(context), "获取成功");
    }

    /**
     * 更新赛事
     * @param eventId 赛事id
     * @param dto 全量dto
     * @return ok
     */
    @PutMapping("/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<Void>> updateEvent(@PathVariable Integer eventId,
            @RequestBody @Valid UpdateEventDTO dto, HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        eventService.updateEvent(dto, context);
        return Result.ok();
    }
}
