package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.data.dto.CreateUserDTO;
import org.hackathon.data.dto.PasswordDTO;
import org.hackathon.data.dto.UpdateUserDTO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.GetUserVO;
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
    @Auth
    public ResponseEntity<Result<GetUserVO>> getUserInfo(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(userService.getUserInfo(jwt.getUserId()), "获取成功");
    }

    /**
     * 修改用户信息
     * @param dto PATCH风格dto，仅修改非null值
     * @return ok
     */
    @PatchMapping
    @Auth
    public ResponseEntity<Result<Void>> updateUserInfo(
            @RequestBody @Valid UpdateUserDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        if (userService.updateUserInfo(dto, jwt.getUserId())) return Result.ok();
        else return Result.noUpdate();
    }

    /**
     * 修改密码
     * @param dto 旧密码+新密码
     * @return ok
     */
    @PutMapping("/password")
    @Auth
    public ResponseEntity<Result<Void>> updatePassword(
            @RequestBody @Valid PasswordDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        userService.updatePassword(dto, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 创建非学生用户
     * @param dto 基本信息+初始密码
     * @return ok
     */
    @PostMapping
    @Auth(onlySuper = true)
    public ResponseEntity<Result<Void>> createNonstudentUser(@RequestBody @Valid CreateUserDTO dto) {
        userService.createNonstudentUser(dto);
        return Result.ok();
    }
}
