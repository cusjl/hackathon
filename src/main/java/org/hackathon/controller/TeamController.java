package org.hackathon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.*;
import org.hackathon.data.vo.*;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return Result.success(teamService.createTeam(dto, ctx), "创建成功");
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

    @PostMapping("/{teamId}/leader/transfer")
    @Require(TEAM_LEADER)
    public ResponseEntity<Result<Void>> transferLeader(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        teamService.changeLeader(ctx, dto.getUserId());
        return Result.ok();
    }

    @PutMapping("/{teamId}/leader")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> setLeader(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        teamService.changeLeader(ctx, dto.getUserId());
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
    public ResponseEntity<Result<Integer>> inviteMember(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        return Result.success(teamService.invite(ctx, dto.getUserId()), "邀请已发送");
    }

    @PostMapping("/{teamId}/members")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> addMember(
            @RequestBody @Valid UserIdDTO dto, Context ctx) {
        teamService.joinTeam(ctx, dto.getUserId());
        return Result.ok();
    }

    @PostMapping("/join-code")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> joinByCode(
            @RequestBody @Valid InviteCodeDTO dto, Context ctx) {
        teamService.joinByCode(dto, ctx);
        return Result.ok();
    }

    @PostMapping("/{teamId}/invite-code")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<String>> refreshInviteCode(Context ctx) {
        return Result.success(teamService.refreshInviteCode(ctx), "邀请码已刷新");
    }

    @GetMapping("/invitations")
    @Require(STUDENT)
    public ResponseEntity<Result<List<TeamInvitationVO>>> invitations(Context ctx) {
        return Result.success(teamService.invitations(ctx.userId()), "获取成功");
    }

    @PostMapping("/{teamId}/invitations/{invitationId}/accept")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> acceptInvitation(
            @PathVariable Integer invitationId, Context ctx) {
        teamService.respondInvitation(ctx, invitationId, true);
        return Result.ok();
    }

    @PostMapping("/{teamId}/invitations/{invitationId}/reject")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> rejectInvitation(
            @PathVariable Integer invitationId, Context ctx) {
        teamService.respondInvitation(ctx, invitationId, false);
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

    @PostMapping("/{teamId}/recruitments")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Integer>> createRecruitment(
            @RequestBody @Valid CreateRecruitmentDTO dto, Context ctx) {
        return Result.success(teamService.createRecruitment(dto, ctx), "发布成功");
    }

    @PutMapping("/{teamId}/recruitments/{recruitmentId}")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> updateRecruitment(
            @PathVariable Integer recruitmentId,
            @RequestBody @Valid UpdateRecruitmentDTO dto, Context ctx) {
        teamService.updateRecruitment(recruitmentId, dto, ctx);
        return Result.ok();
    }

    @DeleteMapping("/{teamId}/recruitments/{recruitmentId}")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> deleteRecruitment(
            @PathVariable Integer recruitmentId, Context ctx) {
        teamService.deleteRecruitment(recruitmentId, ctx);
        return Result.ok();
    }

    @PostMapping("/events/{eventId}/recruitments/list")
    @Require(STUDENT)
    public ResponseEntity<Result<IPage<RecruitmentVO>>> recruitments(
            @PathVariable Integer eventId, @Valid PageParamDTO param,
            @RequestBody @Valid QueryRecruitmentDTO dto) {
        return Result.success(teamService.recruitmentPage(eventId, dto, param), "获取成功");
    }

    @PostMapping("/recruitments/{recruitmentId}/applications")
    @Require(STUDENT)
    public ResponseEntity<Result<Integer>> apply(
            @PathVariable Integer recruitmentId, Context ctx) {
        return Result.success(teamService.apply(recruitmentId, ctx), "申请已提交");
    }

    @GetMapping("/{teamId}/applications")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<List<TeamApplicationVO>>> applications(Context ctx) {
        return Result.success(teamService.applications(ctx), "获取成功");
    }

    @PostMapping("/{teamId}/applications/{applicationId}/accept")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> acceptApplication(
            @PathVariable Integer applicationId, Context ctx) {
        teamService.respondApplication(applicationId, true, ctx);
        return Result.ok();
    }

    @PostMapping("/{teamId}/applications/{applicationId}/reject")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<Void>> rejectApplication(
            @PathVariable Integer applicationId, Context ctx) {
        teamService.respondApplication(applicationId, false, ctx);
        return Result.ok();
    }

    @PutMapping("/events/{eventId}/recommendation")
    @Require(STUDENT)
    public ResponseEntity<Result<Integer>> publishRecommendation(
            @PathVariable Integer eventId,
            @RequestBody @Valid PublishRecommendationDTO dto, Context ctx) {
        return Result.success(teamService.publishRecommendation(eventId, dto, ctx), "自荐已发布");
    }

    @DeleteMapping("/events/{eventId}/recommendation")
    @Require(STUDENT)
    public ResponseEntity<Result<Void>> closeRecommendation(
            @PathVariable Integer eventId, Context ctx) {
        teamService.closeRecommendation(eventId, ctx);
        return Result.ok();
    }

    @PostMapping("/events/{eventId}/recommendations/list")
    @Require(STUDENT)
    public ResponseEntity<Result<IPage<RecommendationVO>>> recommendations(
            @PathVariable Integer eventId, @Valid PageParamDTO param,
            @RequestBody @Valid QueryRecommendationDTO dto) {
        return Result.success(teamService.recommendationPage(eventId, dto, param), "获取成功");
    }

    @PostMapping("/events/{eventId}/admin/list")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<IPage<TeamBriefVO>>> adminTeamList(
            @PathVariable Integer eventId, @Valid PageParamDTO param,
            @RequestBody @Valid QueryTeamDTO dto) {
        return Result.success(teamService.adminTeamPage(eventId, dto, param), "获取成功");
    }
}
