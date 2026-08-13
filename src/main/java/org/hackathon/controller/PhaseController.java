package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.UpdatePhaseConfigDTO;
import org.hackathon.data.dto.UpdatePhaseDTO;
import org.hackathon.data.vo.PhaseInfoVO;
import org.hackathon.data.vo.Result;
import org.hackathon.service.PhaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/phase")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

    /**
     * 获取轮次详情
     * @param phaseId 轮次id
     * @return 详情
     */
    @GetMapping("/{phaseId}")
    @EventAuth(mode = "GUEST", var = "PHASE")
    public ResponseEntity<Result<PhaseInfoVO>> getPhase(@PathVariable String phaseId,
            HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        return Result.success(phaseService.getPhase(context), "获取成功");
    }

    /**
     * 修改轮次信息
     * @param phaseId 轮次id
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{phaseId}")
    @EventAuth(mode = "ADMIN", var = "PHASE")
    public ResponseEntity<Result<Void>> updatePhase(@PathVariable String phaseId,
            @RequestBody @Valid UpdatePhaseDTO dto, HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        phaseService.updatePhase(dto, context);
        return Result.ok();
    }

    /**
     * 修改轮次提交设置
     * @param phaseId 轮次id
     * @param dto 提交设置+version
     * @return ok
     */
    @PutMapping("/{phaseId}/config")
    @EventAuth(mode = "ADMIN", var = "PHASE")
    public ResponseEntity<Result<Void>> updatePhaseConfig(@PathVariable String phaseId,
            @RequestBody @Valid UpdatePhaseConfigDTO dto, HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        phaseService.updatePhaseConfig(dto, context);
        return Result.ok();
    }
}
