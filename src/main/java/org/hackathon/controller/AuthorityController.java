package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.dto.UserIdDTO;
import org.hackathon.data.vo.AuthorityUserVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.service.AuthorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authority")
@RequiredArgsConstructor
public class AuthorityController {

    private final AuthorityService authorityService;

    /**
     * 创建超管权限
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/super")
    @Auth(onlySuper = true)
    public ResponseEntity<Result<Void>> createSuper(@RequestBody @Valid UserIdDTO dto) {
        authorityService.createSuper(dto.getUserId());
        return Result.ok();
    }

    /**
     * 超管自删权限
     * @return ok
     */
    @DeleteMapping("/super-self")
    @Auth(onlySuper = true)
    public ResponseEntity<Result<Void>> deleteSuperSelf(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        authorityService.deleteSuper(jwt.getUserId());
        return Result.ok();
    }

    /**
     * 创建赛管权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/admin/{eventId}")
    @Auth(onlySuper = true)
    public ResponseEntity<Result<Void>> createAdmin(
            @PathVariable Integer eventId, @RequestBody @Valid UserIdDTO dto) {
        authorityService.createAdmin(dto.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 删除赛管权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @DeleteMapping("/admin/{eventId}")
    @Auth(onlySuper = true)
    public ResponseEntity<Result<Void>> deleteAdmin(
            @PathVariable Integer eventId, @RequestBody @Valid UserIdDTO dto) {
        authorityService.deleteAdmin(dto.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 赛管自删权限
     * @param eventId 赛事id
     * @return ok
     */
    @DeleteMapping("/admin-self/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<Void>> deleteAdminSelf(
            @PathVariable Integer eventId, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        authorityService.deleteAdmin(jwt.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 创建评委权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/judge/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<Void>> createJudge(
            @PathVariable Integer eventId, @RequestBody @Valid UserIdDTO dto) {
        authorityService.createJudge(dto.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 删除评委权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @DeleteMapping("/judge/{eventId}")
    @EventAuth(mode = "ADMIN", var = "EVENT")
    public ResponseEntity<Result<Void>> deleteJudge(
            @PathVariable Integer eventId, @RequestBody @Valid UserIdDTO dto) {
        authorityService.deleteJudge(dto.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 评委自删权限
     * @param eventId 赛事id
     * @return ok
     */
    @DeleteMapping("/judge-self/{eventId}")
    @EventAuth(mode = "JUDGE", var = "EVENT")
    public ResponseEntity<Result<Void>> deleteJudgeSelf(
            @PathVariable Integer eventId, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        authorityService.deleteJudge(jwt.getUserId(), eventId);
        return Result.ok();
    }

    /**
     * 查询赛事相关人员
     * @param eventId 赛事id
     * @return 简表
     */
    @GetMapping("/{eventId}")
    @EventAuth(mode = "GUEST", var = "EVENT")
    public ResponseEntity<Result<List<AuthorityUserVO>>> getAuthorityListByEvent(
            @PathVariable Integer eventId) {
        return Result.success(authorityService.getAuthorityListByEvent(eventId), "获取成功");
    }
}
