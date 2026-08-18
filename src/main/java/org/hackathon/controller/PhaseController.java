package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.UpdatePhaseConfigDTO;
import org.hackathon.data.dto.UpdatePhaseDTO;
import org.hackathon.data.vo.PhaseInfoVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.PhaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/phase")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    /**
     * 获取轮次详情
     * @return 详情
     */
    @GetMapping("/{phaseId}")
    public ResponseEntity<Result<PhaseInfoVO>> getPhase(Context ctx) {
        return Result.success(phaseService.getPhase(ctx), "获取成功");
    }

    /**
     * 修改轮次信息
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{phaseId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updatePhase(
            @RequestBody @Valid UpdatePhaseDTO dto, Context ctx) {
        phaseService.updatePhase(dto, ctx);
        return Result.ok();
    }

    /**
     * 修改轮次提交设置
     * @param dto 提交设置+version
     * @return ok
     */
    @PutMapping("/{phaseId}/config")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updatePhaseConfig(
            @RequestBody @Valid UpdatePhaseConfigDTO dto, Context ctx) {
        phaseService.updatePhaseConfig(dto, ctx);
        return Result.ok();
    }

    /**
     * 删除轮次
     * @return ok
     */
    @DeleteMapping("/{phaseId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> deletePhase(Context ctx) {
        phaseService.deletePhase(ctx);
        return Result.ok();
    }
}
