package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.CreateEventDTO;
import org.hackathon.data.dto.CreateTrackDTO;
import org.hackathon.data.dto.UpdateEventDTO;
import org.hackathon.data.vo.CreateEventVO;
import org.hackathon.data.vo.CreateTrackVO;
import org.hackathon.data.vo.GetEventVO;
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
    public ResponseEntity<Result<CreateEventVO>> createEvent(@RequestBody @Valid CreateEventDTO dto){
        return Result.success(new CreateEventVO(eventService.createEvent(dto)), "创建成功");
    }

    /**
     * 创建赛道
     * @param eventId 赛事id
     * @param dto 赛道信息
     * @return 赛道id
     */
    @PostMapping("/{eventId}/track")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<CreateTrackVO>> createTrack(
            @PathVariable Integer eventId, @RequestBody @Valid CreateTrackDTO dto){
        return Result.success(new CreateTrackVO(eventService.createTrack(dto, eventId)), "创建成功");
    }

    /**
     * 获得赛事基本信息
     * @param eventId 赛事id
     * @return 赛事信息及赛道名录
     */
    @GetMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    public ResponseEntity<Result<GetEventVO>> getEvent(@PathVariable Integer eventId){
        return Result.success(eventService.getEvent(eventId), "获取成功");
    }

    /**
     * 更新赛事
     * @param eventId 赛事id
     * @param dto PATCH风格dto
     * @return ok
     */
    @PatchMapping("/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<Void>> updateEvent(
            @PathVariable Integer eventId, @RequestBody @Valid UpdateEventDTO dto) {
        if (eventService.updateEvent(dto, eventId)) return Result.ok();
        else return Result.noUpdate();
    }
}
