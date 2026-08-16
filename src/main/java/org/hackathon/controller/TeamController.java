package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.annotation.TeamAuth;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.CreateTeamDTO;
import org.hackathon.data.dto.UpdateTeamDTO;
import org.hackathon.data.dto.UserIdDTO;
import org.hackathon.data.po.Team;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.TeamIdVO;
import org.hackathon.data.vo.TeamInfoVO;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * 创建队伍
     * @param trackId 赛道id
     * @param dto 队伍信息
     * @return 队伍id
     */
    @PostMapping("/{trackId}")
    @EventAuth(mode = "GUEST", var = "TRACK")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<TeamIdVO>> createTeam(@PathVariable Integer trackId,
            @RequestBody @Valid CreateTeamDTO dto, HttpServletRequest request) {
        EventContext context = (EventContext) request.getAttribute("context");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        TeamIdVO vo = new TeamIdVO(teamService.createTeam(dto, context, jwt.getUserId()));
        return Result.success(vo, "创建成功");
    }

    /**
     * 队员查询队伍
     * @param teamId 队伍id
     * @return vo
     */
    @GetMapping("/{teamId}")
    @TeamAuth
    public ResponseEntity<Result<TeamInfoVO>> getTeam(@PathVariable Integer teamId,
            HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(teamService.getTeam(team, jwt.getUserId()), "获取成功");
    }

    /**
     * 修改队伍信息
     * @param teamId 队伍id
     * @param dto dto
     * @return ok
     */
    @PutMapping("/{teamId}")
    @TeamAuth(onlyLeader = true)
    public ResponseEntity<Result<Void>> updateTeam(@PathVariable Integer teamId,
            @RequestBody @Valid UpdateTeamDTO dto, HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        teamService.updateTeam(dto, team);
        return Result.ok();
    }

    /**
     * 加入队伍
     * @param teamId 队伍id
     * @return ok
     */
    @PostMapping("/{teamId}/join")
    @TeamAuth
    public ResponseEntity<Result<Void>> joinTeam(@PathVariable Integer teamId,
            HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        teamService.joinTeam(team, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 队长邀请入队
     * @param teamId 队伍id
     * @param dto 用户id
     * @return ok
     */
    @PostMapping("/{teamId}/invite")
    @TeamAuth(onlyLeader = true)
    public ResponseEntity<Result<Void>> inviteMember(@PathVariable Integer teamId,
            @RequestBody @Valid UserIdDTO dto, HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        teamService.joinTeam(team, dto.getUserId());
        return Result.ok();
    }

    /**
     * 队员离队
     * @param teamId 队伍id
     * @return ok
     */
    @DeleteMapping("/{teamId}/leave")
    @TeamAuth
    public ResponseEntity<Result<Void>> leaveTeam(@PathVariable Integer teamId,
            HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        teamService.leaveTeam(team, jwt.getUserId());
        return Result.ok();
    }

    /**
     * 队长踢人
     * @param teamId 队伍id
     * @param dto 用户id
     * @return ok
     */
    @DeleteMapping("/{teamId}/kick")
    @TeamAuth(onlyLeader = true)
    public ResponseEntity<Result<Void>> kickMember(@PathVariable Integer teamId,
            @RequestBody @Valid UserIdDTO dto, HttpServletRequest request) {
        Team team = (Team) request.getAttribute("team");
        teamService.leaveTeam(team, dto.getUserId());
        return Result.ok();
    }

    /**
     * 解散队伍
     * @param teamId 队伍id
     * @return ok
     */
    @DeleteMapping("/{teamId}")
    @TeamAuth(onlyLeader = true)
    public ResponseEntity<Result<Void>> deleteTeam(@PathVariable Integer teamId,
            HttpServletRequest request){
        Team team = (Team) request.getAttribute("team");
        teamService.deleteTeam(team);
        return Result.ok();
    }
}
