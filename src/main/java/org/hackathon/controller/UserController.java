package org.hackathon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.*;
import org.hackathon.data.vo.*;
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
     * 外部用户自查信息
     * @return vo
     */
    @GetMapping
    @Auth(onlyExtern = true)
    public ResponseEntity<Result<ExUserInfoVO>> getExUser(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(userService.getExUser(jwt.getUserId()), "获取成功");
    }

    /**
     * 查询用户详情
     * @param userId 用户id
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    @Auth
    public ResponseEntity<Result<UserInfoVO>> getUser(@PathVariable Integer userId) {
        return Result.success(userService.getUserById(userId), "获取成功");
    }

    /**
     * 修改外部用户信息
     * @param dto dto
     * @return ok
     */
    @PutMapping
    @Auth(onlyExtern = true)
    public ResponseEntity<Result<Void>> updateExUser(
            @RequestBody @Valid UpdateExUserDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        userService.updateExUser(dto, jwt.getUserId());
        return Result.ok();
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
    public ResponseEntity<Result<UserIdVO>> createExAdmin(
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
    public ResponseEntity<Result<UserIdVO>> createExUser(
            @PathVariable Integer eventId, @RequestBody @Valid CreateExUserDTO dto) {
        return Result.success(userService.createExJudge(dto, eventId), "创建成功");
    }

    /**
     * 查询分页用户
     * @param param 分页参数
     * @param dto 查询参数
     * @return 分页列表
     */
    @PostMapping("/list")
    @Auth(onlySuper = true)
    public ResponseEntity<Result<IPage<UserBriefVO>>> getUserPage(@Valid PageParamDTO param,
            @RequestBody @Valid QueryUserDTO dto) {
        return Result.success(userService.getUserPage(dto, param), "获取成功");
    }
}
