package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreatePhaseDTO;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.vo.PhaseIdVO;
import org.hackathon.data.vo.PhaseActiveVO;
import org.hackathon.data.vo.TrackInfoVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.TrackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /**
     * 获取赛道详情
     * @return vo
     */
    @GetMapping("/{trackId}")
    public ResponseEntity<Result<TrackInfoVO>> getTrack(Context ctx) {
        return Result.success(trackService.getTrack(ctx), "获取成功");
    }

    /**
     * 更新赛道
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{trackId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updateTrack(
            @RequestBody @Valid UpdateTrackDTO dto, Context ctx) {
        trackService.updateTrack(dto, ctx);
        return Result.ok();
    }

    /**
     * 获取进行中轮次
     * @param trackId 赛道id
     * @return dto，existed = false表示没有
     */
    @GetMapping("/{trackId}/active")
    public ResponseEntity<Result<PhaseActiveVO>> getActivePhase(@PathVariable Integer trackId) {
        return Result.success(trackService.getActivePhase(trackId), "获取成功");
    }

    /**
     * 创建轮次
     * @param dto 轮次信息
     * @return 轮次id
     */
    @PostMapping("/{trackId}/phase")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<PhaseIdVO>> createPhase(
            @RequestBody @Valid CreatePhaseDTO dto, Context ctx) {
        return Result.success(trackService.createPhase(dto, ctx), "创建成功");
    }

    /**
     * 删除赛道
     * @return ok
     */
    @DeleteMapping("/{trackId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> deleteTrack(Context ctx) {
        trackService.deleteTrack(ctx);
        return Result.ok();
    }
}
