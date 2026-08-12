package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.vo.GetActivePhaseVO;
import org.hackathon.data.vo.GetTrackVO;
import org.hackathon.data.vo.Result;
import org.hackathon.service.TrackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /**
     * 获取赛道详情
     * @param trackId 赛道id
     * @return vo
     */
    @GetMapping("/{trackId}")
    @EventAuth(mode = "GUEST", var = "TRACK")
    public ResponseEntity<Result<GetTrackVO>> getTrack(@PathVariable Integer trackId) {
        return Result.success(trackService.getTrack(trackId), "获取成功");
    }

    /**
     * 更新赛道
     * @param trackId 赛道id
     * @param dto PATCH风格dto
     * @return ok
     */
    @PatchMapping("/{trackId}")
    @EventAuth(mode = "ADMIN", var = "TRACK")
    public ResponseEntity<Result<Void>> updateTrack(
            @PathVariable Integer trackId, @RequestBody @Valid UpdateTrackDTO dto) {
        if (trackService.updateTrack(dto, trackId)) return Result.ok();
        else return Result.noUpdate();
    }

    /**
     * 获取进行中轮次
     * @param trackId 赛道id
     * @return dto，existed = false表示没有
     */
    @GetMapping("/{trackId}/active")
    @EventAuth(mode = "GUEST", var = "TRACK")
    public ResponseEntity<Result<GetActivePhaseVO>> getActivePhase(@PathVariable Integer trackId) {
        return Result.success(trackService.getActivePhase(trackId), "获取成功");
    }
}
