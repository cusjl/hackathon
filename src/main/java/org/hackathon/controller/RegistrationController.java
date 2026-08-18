package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.UpdateRegistrationDTO;
import org.hackathon.data.vo.RegistrationVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.security.Require.Window.REGISTRATION;
import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService regService;

    /**
     * 赛道报名
     * @return ok
     */
    @PostMapping("/{trackId}")
    @Require(value = STUDENT, window = REGISTRATION)
    public ResponseEntity<Result<Void>> createRegistration(Context ctx) {
        regService.createRegistration(ctx);
        return Result.ok();
    }

    /**
     * 获取报名信息
     * @return vo
     */
    @GetMapping("/{eventId}")
    @Require(STUDENT)
    public ResponseEntity<Result<RegistrationVO>> getRegistration(Context ctx) {
        return Result.success(regService.getRegistration(ctx), "获取成功");
    }

    /**
     * 更改赛道
     * @param dto 新赛道id+报名version
     * @return ok
     */
    @PutMapping("/{eventId}")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> updateRegistration(
            @RequestBody @Valid UpdateRegistrationDTO dto, Context ctx) {
        regService.updateRegistration(dto, ctx);
        return Result.ok();
    }

    /**
     * 取消报名
     * @return ok
     */
    @DeleteMapping("/{eventId}")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> deleteRegistration(Context ctx) {
        regService.deleteRegistration(ctx);
        return Result.ok();
    }
}
