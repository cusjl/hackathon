package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateTeamDTO;
import org.hackathon.data.dto.UpdateTeamDTO;
import org.hackathon.data.dto.UserIdDTO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.TeamIdVO;
import org.hackathon.data.vo.TeamInfoVO;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * 创建队伍
     * @param dto 队伍信息
     * @return 队伍id
     */
    @PostMapping("/{trackId}")
    @Require(STUDENT)
    public ResponseEntity<Result<TeamIdVO>> createTeam(
            @RequestBody @Valid CreateTeamDTO dto, Context ctx) {
        return Result.success(new TeamIdVO(teamService.createTeam(dto, ctx)), "创建成功");
    }

    /**
     * 队员或赛管查询队伍
     * @return vo
     */
    @GetMapping("/{teamId}")
    @Require({TEAM_MEMBER, EVENT_ADMIN})
    public ResponseEntity<Result<TeamInfoVO>> getTeam(Context ctx) {
        return Result.success(teamService.getTeam(ctx), "获取成功");
    }

    /**
     * 修改队伍信息
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{teamId}")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> updateTeam(
            @RequestBody @Valid UpdateTeamDTO dto, Context ctx) {
        teamService.updateTeam(dto, ctx);
        return Result.ok();
    }

    /**
     * 加入队伍
     * @return ok
     */
    @PostMapping("/{teamId}/join")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> joinTeam(Context ctx) {
        teamService.joinTeam(ctx, ctx.userId());
        return Result.ok();
    }

    /**
     * 队长邀请入队
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/{teamId}/invite")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> inviteMember(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        teamService.joinTeam(ctx, dto.getUserId());
        return Result.ok();
    }

    /**
     * 队员离队
     * @return ok
     */
    @DeleteMapping("/{teamId}/leave")
    @Require(TEAM_MEMBER)
    public ResponseEntity<Result<Void>> leaveTeam(Context ctx) {
        teamService.leaveTeam(ctx, ctx.userId());
        return Result.ok();
    }

    /**
     * 队长踢人
     * @param dto 用户id
     * @return ok
     */
    @DeleteMapping("/{teamId}/kick")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> kickMember(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        teamService.leaveTeam(ctx, dto.getUserId());
        return Result.ok();
    }

    /**
     * 解散队伍
     * @return ok
     */
    @DeleteMapping("/{teamId}")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> deleteTeam(Context ctx) {
        teamService.deleteTeam(ctx);
        return Result.ok();
    }
}
