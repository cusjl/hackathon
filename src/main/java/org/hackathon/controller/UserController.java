package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.CreateExUserDTO;
import org.hackathon.data.dto.UpdateExUserDTO;
import org.hackathon.data.dto.UpdatePasswordDTO;
import org.hackathon.data.vo.CreateExUserVO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.GetExUserVO;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 获取用户信息
     * @return vo
     */
    @GetMapping
    @Auth(onlyExtern = true)
    public ResponseEntity<Result<GetExUserVO>> getExUser(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(userService.getExUser(jwt.getUserId()), "获取成功");
    }

    /**
     * 修改用户信息
     * @param dto PATCH风格dto，仅修改非null值
     * @return ok/noUpdate
     */
    @PatchMapping
    @Auth(onlyExtern = true)
    public ResponseEntity<Result<Void>> updateExUser(
            @RequestBody @Valid UpdateExUserDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        if (userService.updateExUser(dto, jwt.getUserId())) return Result.ok();
        else return Result.noUpdate();
    }

    /**
     * 修改密码
     * @param dto 旧密码+新密码
     * @return ok/noUpdate
     */
    @PutMapping("/password")
    @Auth
    public ResponseEntity<Result<Void>> updatePassword(
            @RequestBody @Valid UpdatePasswordDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        userService.updatePassword(dto, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 创建赛管
     * @param eventId 赛事id
     * @param dto 基本信息+初始密码+是否校内
     * @return 新用户id
     */
    @PostMapping("/admin/{eventId}")
    @Auth(onlySuper = true)
    @EventAuth(mode = "GUEST", var = "EVENT")
    public ResponseEntity<Result<CreateExUserVO>> createExAdmin(
            @PathVariable Integer eventId, @RequestBody @Valid CreateExUserDTO dto) {
        return Result.success(userService.createExAdmin(dto, eventId), "创建成功");
    }

    /**
     * 创建评委
     * @param eventId 赛事id
     * @param dto 基本信息+初始密码+是否校内
     * @return 新用户id
     */
    @PostMapping("/judge/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<CreateExUserVO>> createExUser(
            @PathVariable Integer eventId, @RequestBody @Valid CreateExUserDTO dto) {
        return Result.success(userService.createExJudge(dto, eventId), "创建成功");
    }
}
