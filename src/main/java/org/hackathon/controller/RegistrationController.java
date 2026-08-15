package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.UpdateRegistrationDTO;
import org.hackathon.data.vo.RegistrationVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService regService;

    /**
     * 赛道报名
     * @param trackId 赛道id
     * @return ok
     */
    @PostMapping("/{trackId}")
    @EventAuth(mode = "GUEST", var = "TRACK")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<Void>> createRegistration(@PathVariable Integer trackId,
            HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        regService.createRegistration(context, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 获取报名信息
     * @param eventId 赛事id
     * @return vo
     */
    @GetMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<RegistrationVO>> getRegistration(@PathVariable Integer eventId,
            HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(regService.getRegistration(context, jwt.getUserId()), "获取成功");
    }

    /**
     * 更改赛道
     * @param eventId 赛事id
     * @param dto 新赛道id+报名version
     * @return ok
     */
    @PutMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<Void>> updateRegistration(@PathVariable Integer eventId,
            @RequestBody @Valid UpdateRegistrationDTO dto, HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        regService.updateRegistration(dto, context, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 取消报名
     * @param eventId 赛事id
     * @return ok
     */
    @DeleteMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<Void>> deleteRegistration(@PathVariable Integer eventId,
            HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        regService.deleteRegistration(context, jwt.getUserId());
        return Result.ok();
    }
}
