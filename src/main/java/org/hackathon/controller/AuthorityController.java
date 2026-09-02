package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.UserIdDTO;
import org.hackathon.data.vo.*;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.AuthorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.hackathon.security.Role.*;

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
    @Require(SUPER)
    public ResponseEntity<Result<Void>> createSuper(@RequestBody @Valid UserIdDTO dto) {
        authorityService.createSuper(dto.getUserId());
        return Result.ok();
    }

    /**
     * 超管自删权限
     * @return ok
     */
    @DeleteMapping("/super-self")
    @Require(SUPER)
    public ResponseEntity<Result<Void>> deleteSuperSelf(Context ctx) {
        authorityService.deleteSuper(ctx.userId());
        return Result.ok();
    }

    /**
     * 查询当前用户担任赛事管理员的赛事
     * @return 管理赛事简表
     */
    @GetMapping("/admin-self")
    @Require(LOGGED_IN)
    public ResponseEntity<Result<List<AuthorityEventVO>>> getCurrentAdminEvents(Context ctx) {
        return Result.success(authorityService.getAdminEventListByUser(ctx.userId()), "获取成功");
    }

    /**
     * 创建赛管权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/admin/{eventId}")
    @Require(SUPER)
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
    @Require(SUPER)
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
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> deleteAdminSelf(
            @PathVariable Integer eventId, Context ctx) {
        authorityService.deleteAdmin(ctx.userId(), eventId);
        return Result.ok();
    }

    /**
     * 创建评委权限
     * @param eventId 赛事id
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/judge/{eventId}")
    @Require(EVENT_ADMIN)
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
    @Require(EVENT_ADMIN)
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
    @Require(EVENT_JUDGE)
    public ResponseEntity<Result<Void>> deleteJudgeSelf(
            @PathVariable Integer eventId, Context ctx) {
        authorityService.deleteJudge(ctx.userId(), eventId);
        return Result.ok();
    }

    /**
     * 查询赛事相关人员
     * @param eventId 赛事id
     * @return 简表
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<Result<List<AuthorityUserVO>>> getAuthorityListByEvent(
            @PathVariable Integer eventId) {
        return Result.success(authorityService.getAuthorityListByEvent(eventId), "获取成功");
    }

    /**
     * 查询用户所有权限
     * @param userId 用户id
     * @return 列表
     */
    @GetMapping("/user/{userId}")
    @Require(SUPER)
    public ResponseEntity<Result<List<AuthorityEventVO>>> getAuthorityListByUser(
            @PathVariable Integer userId) {
        return Result.success(authorityService.getAuthorityListByUserWithCheck(userId), "获取成功");
    }
}
