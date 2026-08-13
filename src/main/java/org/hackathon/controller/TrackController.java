package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.CreatePhaseDTO;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.vo.PhaseIdVO;
import org.hackathon.data.vo.PhaseActiveVO;
import org.hackathon.data.vo.TrackInfoVO;
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
    public ResponseEntity<Result<TrackInfoVO>> getTrack(@PathVariable Integer trackId) {
        return Result.success(trackService.getTrack(trackId), "获取成功");
    }

    /**
     * 更新赛道
     * @param trackId 赛道id
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{trackId}")
    @EventAuth(mode = "ADMIN", var = "TRACK")
    public ResponseEntity<Result<Void>> updateTrack(
            @PathVariable Integer trackId, @RequestBody @Valid UpdateTrackDTO dto) {
        trackService.updateTrack(dto, trackId);
        return Result.ok();
    }

    /**
     * 获取进行中轮次
     * @param trackId 赛道id
     * @return dto，existed = false表示没有
     */
    @GetMapping("/{trackId}/active")
    @EventAuth(mode = "GUEST", var = "TRACK")
    public ResponseEntity<Result<PhaseActiveVO>> getActivePhase(@PathVariable Integer trackId) {
        return Result.success(trackService.getActivePhase(trackId), "获取成功");
    }

    /**
     * 创建轮次
     * @param trackId 赛道id
     * @param dto 轮次信息
     * @return 轮次id
     */
    @PostMapping("/{trackId}")
    @EventAuth(mode = "ADMIN", var = "TRACK")
    public ResponseEntity<Result<PhaseIdVO>> createPhase(
            @PathVariable Integer trackId, @RequestBody @Valid CreatePhaseDTO dto) {
        return Result.success(trackService.createPhase(dto, trackId), "创建成功");
    }
}
