package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.hackathon.config.GlobalProperties;
import org.hackathon.data.dto.*;
import org.hackathon.data.enums.*;
import org.hackathon.data.po.*;
import org.hackathon.data.vo.*;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.*;
import org.hackathon.security.Context;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationMapper regMapper;
    private final TeamMapper teamMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeamRecruitmentMapper recruitmentMapper;
    private final TeamApplicationMapper applicationMapper;
    private final TeamInvitationMapper invitationMapper;
    private final StudentRecommendationMapper recommendationMapper;
    private final NotificationService notificationService;
    private final GlobalProperties globalProperties;

    private String newInviteCode() {
        StringBuilder code = new StringBuilder(12);
        for (int i = 0; i < 12; i++) code.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        return code.toString();
    }

    private void requireBeforeLive(Event event) {
        if (event.getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
    }

    private int maxSize(Event event) {
        return event.getTeamMaxSize() == null ? globalProperties.teamCapacity() : event.getTeamMaxSize();
    }

    private List<String> splitTags(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private String joinTags(List<String> values) {
        if (values == null) return null;
        String result = values.stream().filter(Objects::nonNull).map(String::trim)
                .filter(StringUtils::hasText).map(v -> v.replace(",", ""))
                .distinct().collect(Collectors.joining(","));
        return result.isEmpty() ? null : result;
    }

    @Transactional
    public TeamIdVO createTeam(CreateTeamDTO dto, Context ctx) {
        Integer userId = ctx.userId();
        Event event = ctx.event();
        Integer trackId = ctx.track().getTrackId();
        requireBeforeLive(event);
        Registration registration = registration(userId, event.getEventId());
        if (registration == null || !registration.getTrackId().equals(trackId)) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        if (registration.getTeamId() != null) throw new BusinessException(ResultCode.ALREADY_TEAMED);

        String inviteCode = newInviteCode();
        Team team = new Team(null, dto.getName(), event.getEventId(), trackId, userId,
                1, dto.getIntroduction(), dto.getType(), TeamStatus.ACTIVE, null, inviteCode,
                1, LocalDateTime.now(), LocalDateTime.now());
        try {
            teamMapper.insert(team);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.TEAM_NAME_CONFLICT);
        }
        registration.setTeamId(team.getTeamId());
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        closeRecommendation(event.getEventId(), userId);
        return new TeamIdVO(team.getTeamId(), inviteCode);
    }

    private Registration registration(Integer userId, Integer eventId) {
        return regMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId).eq(Registration::getEventId, eventId));
    }

    private List<Integer> getMemberIds(Integer teamId) {
        return regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getTeamId, teamId).select(Registration::getUserId))
                .stream().map(Registration::getUserId).toList();
    }

    private List<TeamMemberVO> getTeamMembers(Integer teamId, Integer leaderId) {
        List<Integer> ids = getMemberIds(teamId);
        if (ids.isEmpty()) return List.of();
        Map<Integer, String> names = userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
        return studentMapper.selectByIds(ids).stream().map(po -> new TeamMemberVO(
                po.getUserId(), names.get(po.getUserId()), po.getCasId(), po.getCampus(), po.getMajor(),
                po.getTagsAsList(), po.getUserId().equals(leaderId))).toList();
    }

    public TeamInfoVO getTeam(Context ctx) {
        Team team = ctx.team();
        TeamInfoVO vo = new TeamInfoVO();
        BeanUtils.copyProperties(team, vo);
        vo.setEventName(eventMapper.selectById(team.getEventId()).getName());
        vo.setTrackName(trackMapper.selectById(team.getTrackId()).getName());
        vo.setMembers(getTeamMembers(team.getTeamId(), team.getLeaderId()));
        return vo;
    }

    public void updateTeam(UpdateTeamDTO dto, Context ctx) {
        Team team = ctx.team();
        requireBeforeLive(eventMapper.selectById(team.getEventId()));
        if (team.getSize() > 1 && !dto.getType().equals(TeamEnum.CROSS_CAMPUS)) {
            List<Student> members = studentMapper.selectByIds(getMemberIds(team.getTeamId()));
            long campus = members.stream().map(Student::getCampus).distinct().count();
            long major = members.stream().map(Student::getMajor).distinct().count();
            if (campus > 1 || (major > 1 && dto.getType().equals(TeamEnum.SAME_MAJOR))) {
                throw new BusinessException(ResultCode.TEAM_TYPE_CONFLICT);
            }
        }
        team.setName(dto.getName());
        team.setIntroduction(dto.getIntroduction());
        team.setType(dto.getType());
        team.setVersion(dto.getVersion());
        team.setUpdateTime(LocalDateTime.now());
        try {
            if (teamMapper.updateById(team) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.TEAM_NAME_CONFLICT);
        }
    }

    public void changeLeader(Context ctx, Integer userId) {
        Team team = ctx.team();
        requireBeforeLive(eventMapper.selectById(team.getEventId()));
        if (!regMapper.existsTeamMember(userId, team.getTeamId())) {
            throw new BusinessException(ResultCode.NOT_TEAM_MEMBER);
        }
        if (team.getLeaderId().equals(userId)) {
            throw new BusinessException(ResultCode.LEADER_UNCHANGED);
        }
        Integer previousLeaderId = team.getLeaderId();
        team.setLeaderId(userId);
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        notificationService.push(userId, NoticeType.TEAM_LEADER_CHANGED, "你已成为队长",
                "你已成为队伍“%s”的新队长".formatted(team.getName()),
                team.getEventId(), null, null, team.getTeamId());
        notificationService.push(previousLeaderId, NoticeType.TEAM_LEADER_CHANGED, "队长已变更",
                "队伍“%s”的队长职责已转交".formatted(team.getName()),
                team.getEventId(), null, null, team.getTeamId());
    }

    private void validateCandidate(Team team, Integer userId) {
        Event event = eventMapper.selectById(team.getEventId());
        requireBeforeLive(event);
        if (team.getSize() >= maxSize(event)) throw new BusinessException(ResultCode.TEAM_ALREADY_FULL);
        Registration registration = registration(userId, team.getEventId());
        if (registration == null) throw new BusinessException(ResultCode.NOT_REGISTERED);
        if (!registration.getTrackId().equals(team.getTrackId())) {
            throw new BusinessException(ResultCode.TEAM_TRACK_CONFLICT);
        }
        if (registration.getTeamId() != null) throw new BusinessException(ResultCode.ALREADY_TEAMED);
        Student leader = studentMapper.selectById(team.getLeaderId());
        Student student = studentMapper.selectById(userId);
        if (student == null) throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        if (team.getType() == TeamEnum.SAME_MAJOR && !Objects.equals(student.getMajor(), leader.getMajor())) {
            throw new BusinessException(ResultCode.TEAM_TYPE_CONFLICT);
        }
        if (team.getType() != TeamEnum.CROSS_CAMPUS && !Objects.equals(student.getCampus(), leader.getCampus())) {
            throw new BusinessException(ResultCode.TEAM_TYPE_CONFLICT);
        }
    }

    @Transactional
    public void joinTeam(Context ctx, Integer userId) {
        Team team = ctx.team();
        validateCandidate(team, userId);
        Registration registration = registration(userId, team.getEventId());
        team.setSize(team.getSize() + 1);
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        registration.setTeamId(team.getTeamId());
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        closeRecommendation(team.getEventId(), userId);
        if (team.getSize() >= maxSize(eventMapper.selectById(team.getEventId()))) {
            recruitmentMapper.update(new LambdaUpdateWrapper<TeamRecruitment>()
                    .eq(TeamRecruitment::getTeamId, team.getTeamId())
                    .eq(TeamRecruitment::getOpenFlag, true)
                    .set(TeamRecruitment::getOpenFlag, false)
                    .set(TeamRecruitment::getUpdateTime, LocalDateTime.now()));
        }
    }

    private void closeRecommendation(Integer eventId, Integer userId) {
        recommendationMapper.update(new LambdaUpdateWrapper<StudentRecommendation>()
                .eq(StudentRecommendation::getEventId, eventId)
                .eq(StudentRecommendation::getUserId, userId)
                .set(StudentRecommendation::getOpenFlag, false)
                .set(StudentRecommendation::getUpdateTime, LocalDateTime.now()));
    }

    @Transactional
    public void leaveTeam(Context ctx, Integer userId) {
        Team team = ctx.team();
        requireBeforeLive(eventMapper.selectById(team.getEventId()));
        if (team.getLeaderId().equals(userId)) throw new BusinessException(ResultCode.LEADER_LEAVING);
        Registration registration = regMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId).eq(Registration::getTeamId, team.getTeamId()));
        if (registration == null) throw new BusinessException(ResultCode.NOT_TEAM_MEMBER);
        team.setSize(team.getSize() - 1);
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        registration.setTeamId(null);
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
    }

    @Transactional
    public void deleteTeam(Context ctx) {
        Team team = ctx.team();
        requireBeforeLive(eventMapper.selectById(team.getEventId()));
        regMapper.update(new LambdaUpdateWrapper<Registration>()
                .eq(Registration::getTeamId, team.getTeamId()).set(Registration::getTeamId, null)
                .set(Registration::getUpdateTime, LocalDateTime.now()));
        teamMapper.deleteById(team);
    }

    public String refreshInviteCode(Context ctx) {
        Team team = ctx.team();
        requireBeforeLive(eventMapper.selectById(team.getEventId()));
        team.setInviteCode(newInviteCode());
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        return team.getInviteCode();
    }

    @Transactional
    public void joinByCode(InviteCodeDTO dto, Context ctx) {
        Team team = teamMapper.selectOne(new LambdaQueryWrapper<Team>().eq(Team::getInviteCode, dto.getCode()));
        if (team == null) throw new BusinessException(ResultCode.INVITE_CODE_INVALID);
        joinTeam(ctx.anchor(Map.of("teamId", team.getTeamId())), ctx.userId());
    }

    @Transactional
    public Integer invite(Context ctx, Integer userId) {
        Team team = ctx.team();
        validateCandidate(team, userId);
        TeamInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<TeamInvitation>()
                .eq(TeamInvitation::getTeamId, team.getTeamId()).eq(TeamInvitation::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        if (invitation == null) {
            invitation = new TeamInvitation(null, team.getTeamId(), userId, ctx.userId(),
                    TeamFlowStatus.PENDING, now, now);
            invitationMapper.insert(invitation);
        } else {
            if (invitation.getStatus() == TeamFlowStatus.PENDING) {
                throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "该用户已有待处理邀请");
            }
            invitation.setInviterId(ctx.userId());
            invitation.setStatus(TeamFlowStatus.PENDING);
            invitation.setUpdateTime(now);
            invitationMapper.updateById(invitation);
        }
        notificationService.push(userId, NoticeType.TEAM_INVITATION, "收到组队邀请",
                "“%s”邀请你加入队伍“%s”".formatted(ctx.name(), team.getName()),
                team.getEventId(), null, null, invitation.getInvitationId());
        return invitation.getInvitationId();
    }

    public List<TeamInvitationVO> invitations(Integer userId) {
        return invitationMapper.selectList(new LambdaQueryWrapper<TeamInvitation>()
                .eq(TeamInvitation::getUserId, userId).orderByDesc(TeamInvitation::getUpdateTime))
                .stream().map(this::invitationVO).toList();
    }

    private TeamInvitationVO invitationVO(TeamInvitation po) {
        Team team = teamMapper.selectById(po.getTeamId());
        TeamInvitationVO vo = new TeamInvitationVO();
        BeanUtils.copyProperties(po, vo);
        if (team != null) {
            vo.setTeamName(team.getName());
            vo.setEventId(team.getEventId());
            vo.setTrackId(team.getTrackId());
            Event event = eventMapper.selectById(team.getEventId());
            Track track = trackMapper.selectById(team.getTrackId());
            vo.setEventName(event == null ? null : event.getName());
            vo.setTrackName(track == null ? null : track.getName());
        }
        User inviter = userMapper.selectById(po.getInviterId());
        vo.setInviterName(inviter == null ? null : inviter.getName());
        return vo;
    }

    @Transactional
    public void respondInvitation(Context ctx, Integer invitationId, boolean accept) {
        TeamInvitation invitation = invitationMapper.selectById(invitationId);
        if (invitation == null || !invitation.getTeamId().equals(ctx.team().getTeamId())
                || !invitation.getUserId().equals(ctx.userId())) {
            throw new BusinessException(ResultCode.INVITATION_NOT_FOUND);
        }
        if (invitation.getStatus() != TeamFlowStatus.PENDING) {
            throw new BusinessException(ResultCode.INVITATION_ALREADY_HANDLED);
        }
        if (accept) joinTeam(ctx, ctx.userId());
        invitation.setStatus(accept ? TeamFlowStatus.ACCEPTED : TeamFlowStatus.REJECTED);
        invitation.setUpdateTime(LocalDateTime.now());
        invitationMapper.updateById(invitation);
    }

    @Transactional
    public Integer createRecruitment(CreateRecruitmentDTO dto, Context ctx) {
        Team team = ctx.team();
        Event event = eventMapper.selectById(team.getEventId());
        requireBeforeLive(event);
        if (dto.getVacancies() > maxSize(event) - team.getSize()) {
            throw new BusinessException(ResultCode.RECRUITMENT_CAPACITY_CONFLICT);
        }
        TeamRecruitment recruitment = new TeamRecruitment(null, team.getTeamId(), dto.getTitle(),
                dto.getDescription(), joinTags(dto.getRequiredTags()), dto.getVacancies(), true, 1,
                LocalDateTime.now(), LocalDateTime.now());
        recruitmentMapper.insert(recruitment);
        return recruitment.getRecruitmentId();
    }

    public void updateRecruitment(Integer recruitmentId, UpdateRecruitmentDTO dto, Context ctx) {
        TeamRecruitment recruitment = ownedRecruitment(recruitmentId, ctx.team().getTeamId());
        Event event = eventMapper.selectById(ctx.team().getEventId());
        requireBeforeLive(event);
        if (Boolean.TRUE.equals(dto.getOpen())
                && dto.getVacancies() > maxSize(event) - ctx.team().getSize()) {
            throw new BusinessException(ResultCode.RECRUITMENT_CAPACITY_CONFLICT);
        }
        recruitment.setTitle(dto.getTitle());
        recruitment.setDescription(dto.getDescription());
        recruitment.setRequiredTags(joinTags(dto.getRequiredTags()));
        recruitment.setVacancies(dto.getVacancies());
        recruitment.setOpenFlag(dto.getOpen());
        recruitment.setVersion(dto.getVersion());
        recruitment.setUpdateTime(LocalDateTime.now());
        if (recruitmentMapper.updateById(recruitment) == 0) throw new BusinessException(ResultCode.RESOURCE_UPDATED);
    }

    public void deleteRecruitment(Integer recruitmentId, Context ctx) {
        TeamRecruitment recruitment = ownedRecruitment(recruitmentId, ctx.team().getTeamId());
        requireBeforeLive(eventMapper.selectById(ctx.team().getEventId()));
        recruitmentMapper.deleteById(recruitment);
    }

    private TeamRecruitment ownedRecruitment(Integer id, Integer teamId) {
        TeamRecruitment recruitment = recruitmentMapper.selectById(id);
        if (recruitment == null || !recruitment.getTeamId().equals(teamId)) {
            throw new BusinessException(ResultCode.RECRUITMENT_NOT_FOUND);
        }
        return recruitment;
    }

    public IPage<RecruitmentVO> recruitmentPage(Integer eventId, QueryRecruitmentDTO dto, PageParamDTO param) {
        LambdaQueryWrapper<Team> teamQuery = new LambdaQueryWrapper<Team>()
                .eq(Team::getEventId, eventId)
                .eq(dto.getTrackId() != null, Team::getTrackId, dto.getTrackId())
                .select(Team::getTeamId);
        List<Integer> teamIds = teamMapper.selectList(teamQuery).stream().map(Team::getTeamId).toList();
        if (teamIds.isEmpty()) return new Page<>(param.getPage(), param.getSize());
        LambdaQueryWrapper<TeamRecruitment> wrapper = new LambdaQueryWrapper<TeamRecruitment>()
                .in(TeamRecruitment::getTeamId, teamIds).eq(TeamRecruitment::getOpenFlag, true)
                .and(StringUtils.hasText(dto.getKeyword()), w -> w.like(TeamRecruitment::getTitle, dto.getKeyword())
                        .or().like(TeamRecruitment::getDescription, dto.getKeyword()))
                .like(StringUtils.hasText(dto.getSkill()), TeamRecruitment::getRequiredTags, dto.getSkill())
                .orderByDesc(TeamRecruitment::getUpdateTime);
        return recruitmentMapper.selectPage(new Page<>(param.getPage(), param.getSize()), wrapper)
                .convert(this::recruitmentVO);
    }

    private RecruitmentVO recruitmentVO(TeamRecruitment po) {
        Team team = teamMapper.selectById(po.getTeamId());
        RecruitmentVO vo = new RecruitmentVO();
        BeanUtils.copyProperties(po, vo);
        vo.setOpen(po.getOpenFlag());
        vo.setRequiredTags(splitTags(po.getRequiredTags()));
        if (team != null) {
            vo.setTeamName(team.getName());
            vo.setEventId(team.getEventId());
            vo.setTrackId(team.getTrackId());
            vo.setCurrentSize(team.getSize());
            Event event = eventMapper.selectById(team.getEventId());
            vo.setMaxSize(event == null ? null : maxSize(event));
            Track track = trackMapper.selectById(team.getTrackId());
            vo.setTrackName(track == null ? null : track.getName());
        }
        return vo;
    }

    @Transactional
    public Integer apply(Integer recruitmentId, Context ctx) {
        TeamRecruitment recruitment = recruitmentMapper.selectById(recruitmentId);
        if (recruitment == null) throw new BusinessException(ResultCode.RECRUITMENT_NOT_FOUND);
        if (!Boolean.TRUE.equals(recruitment.getOpenFlag())) throw new BusinessException(ResultCode.RECRUITMENT_CLOSED);
        Team team = teamMapper.selectById(recruitment.getTeamId());
        validateCandidate(team, ctx.userId());
        TeamApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<TeamApplication>()
                .eq(TeamApplication::getRecruitmentId, recruitmentId)
                .eq(TeamApplication::getUserId, ctx.userId()));
        LocalDateTime now = LocalDateTime.now();
        if (application == null) {
            application = new TeamApplication(null, recruitmentId, team.getTeamId(), ctx.userId(),
                    TeamFlowStatus.PENDING, now, now);
            applicationMapper.insert(application);
        } else {
            if (application.getStatus() == TeamFlowStatus.PENDING) {
                throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "已有待处理入队申请");
            }
            application.setStatus(TeamFlowStatus.PENDING);
            application.setUpdateTime(now);
            applicationMapper.updateById(application);
        }
        notificationService.push(team.getLeaderId(), NoticeType.TEAM_APPLICATION, "收到入队申请",
                "%s申请加入队伍“%s”".formatted(ctx.name(), team.getName()),
                team.getEventId(), null, null, application.getApplicationId());
        return application.getApplicationId();
    }

    public List<TeamApplicationVO> applications(Context ctx) {
        return applicationMapper.selectList(new LambdaQueryWrapper<TeamApplication>()
                .eq(TeamApplication::getTeamId, ctx.team().getTeamId())
                .orderByDesc(TeamApplication::getUpdateTime)).stream().map(this::applicationVO).toList();
    }

    private TeamApplicationVO applicationVO(TeamApplication po) {
        TeamApplicationVO vo = new TeamApplicationVO();
        BeanUtils.copyProperties(po, vo);
        User user = userMapper.selectById(po.getUserId());
        Student student = studentMapper.selectById(po.getUserId());
        vo.setUserName(user == null ? null : user.getName());
        if (student != null) {
            vo.setCampus(student.getCampus());
            vo.setMajor(student.getMajor());
            vo.setTags(student.getTagsAsList());
        }
        return vo;
    }

    @Transactional
    public void respondApplication(Integer applicationId, boolean accept, Context ctx) {
        TeamApplication application = applicationMapper.selectById(applicationId);
        if (application == null || !application.getTeamId().equals(ctx.team().getTeamId())) {
            throw new BusinessException(ResultCode.APPLICATION_NOT_FOUND);
        }
        if (application.getStatus() != TeamFlowStatus.PENDING) {
            throw new BusinessException(ResultCode.APPLICATION_ALREADY_HANDLED);
        }
        if (accept) {
            joinTeam(ctx, application.getUserId());
            TeamRecruitment recruitment = recruitmentMapper.selectById(application.getRecruitmentId());
            if (recruitment != null) {
                int remaining = Math.max(0, recruitment.getVacancies() - 1);
                recruitment.setVacancies(remaining);
                recruitment.setOpenFlag(remaining > 0 && ctx.team().getSize() < maxSize(eventMapper.selectById(ctx.team().getEventId())));
                recruitment.setUpdateTime(LocalDateTime.now());
                recruitmentMapper.updateById(recruitment);
            }
        }
        application.setStatus(accept ? TeamFlowStatus.ACCEPTED : TeamFlowStatus.REJECTED);
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);
        notificationService.push(application.getUserId(), NoticeType.TEAM_APPLICATION_RESULT,
                accept ? "入队申请已通过" : "入队申请未通过",
                "队伍“%s”已处理你的入队申请".formatted(ctx.team().getName()),
                ctx.team().getEventId(), null, null, applicationId);
    }

    @Transactional
    public Integer publishRecommendation(Integer eventId, PublishRecommendationDTO dto, Context ctx) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        requireBeforeLive(event);
        Registration registration = registration(ctx.userId(), eventId);
        if (registration == null) throw new BusinessException(ResultCode.NOT_REGISTERED);
        if (registration.getTeamId() != null) throw new BusinessException(ResultCode.ALREADY_TEAMED);
        StudentRecommendation recommendation = recommendationMapper.selectOne(
                new LambdaQueryWrapper<StudentRecommendation>().eq(StudentRecommendation::getEventId, eventId)
                        .eq(StudentRecommendation::getUserId, ctx.userId()));
        LocalDateTime now = LocalDateTime.now();
        if (recommendation == null) {
            recommendation = new StudentRecommendation(null, eventId, registration.getTrackId(), ctx.userId(),
                    dto.getIntroduction(), joinTags(dto.getSkills()), true, 1, now, now);
            recommendationMapper.insert(recommendation);
        } else {
            recommendation.setTrackId(registration.getTrackId());
            recommendation.setIntroduction(dto.getIntroduction());
            recommendation.setSkills(joinTags(dto.getSkills()));
            recommendation.setOpenFlag(true);
            recommendation.setUpdateTime(now);
            recommendationMapper.updateById(recommendation);
        }
        return recommendation.getRecommendationId();
    }

    public void closeRecommendation(Integer eventId, Context ctx) {
        StudentRecommendation recommendation = recommendationMapper.selectOne(
                new LambdaQueryWrapper<StudentRecommendation>().eq(StudentRecommendation::getEventId, eventId)
                        .eq(StudentRecommendation::getUserId, ctx.userId()));
        if (recommendation == null) throw new BusinessException(ResultCode.RECOMMENDATION_NOT_FOUND);
        recommendation.setOpenFlag(false);
        recommendation.setUpdateTime(LocalDateTime.now());
        recommendationMapper.updateById(recommendation);
    }

    public IPage<RecommendationVO> recommendationPage(Integer eventId, QueryRecommendationDTO dto,
                                                       PageParamDTO param) {
        LambdaQueryWrapper<StudentRecommendation> wrapper = new LambdaQueryWrapper<StudentRecommendation>()
                .eq(StudentRecommendation::getEventId, eventId).eq(StudentRecommendation::getOpenFlag, true)
                .eq(dto.getTrackId() != null, StudentRecommendation::getTrackId, dto.getTrackId())
                .like(StringUtils.hasText(dto.getSkill()), StudentRecommendation::getSkills, dto.getSkill())
                .like(StringUtils.hasText(dto.getKeyword()), StudentRecommendation::getIntroduction, dto.getKeyword())
                .orderByDesc(StudentRecommendation::getUpdateTime);
        return recommendationMapper.selectPage(new Page<>(param.getPage(), param.getSize()), wrapper)
                .convert(this::recommendationVO);
    }

    private RecommendationVO recommendationVO(StudentRecommendation po) {
        RecommendationVO vo = new RecommendationVO();
        BeanUtils.copyProperties(po, vo);
        vo.setSkills(splitTags(po.getSkills()));
        Track track = trackMapper.selectById(po.getTrackId());
        User user = userMapper.selectById(po.getUserId());
        Student student = studentMapper.selectById(po.getUserId());
        vo.setTrackName(track == null ? null : track.getName());
        vo.setUserName(user == null ? null : user.getName());
        if (student != null) {
            vo.setCampus(student.getCampus());
            vo.setMajor(student.getMajor());
            vo.setProfileTags(student.getTagsAsList());
        }
        return vo;
    }

    public IPage<TeamBriefVO> adminTeamPage(Integer eventId, QueryTeamDTO dto, PageParamDTO param) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<Team>()
                .eq(Team::getEventId, eventId)
                .eq(dto.getTrackId() != null, Team::getTrackId, dto.getTrackId())
                .eq(dto.getStatus() != null, Team::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getName()), Team::getName, dto.getName())
                .orderByDesc(Team::getUpdateTime);
        return teamMapper.selectPage(new Page<>(param.getPage(), param.getSize()), wrapper)
                .convert(this::teamBriefVO);
    }

    public IPage<TeamBriefVO> joinableTeamPage(Integer eventId, QueryJoinableTeamDTO dto,
                                                PageParamDTO param, Integer userId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        requireBeforeLive(event);
        Registration registration = registration(userId, eventId);
        if (registration == null) throw new BusinessException(ResultCode.NOT_REGISTERED);
        if (registration.getTeamId() != null) throw new BusinessException(ResultCode.ALREADY_TEAMED);

        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<Team>()
                .eq(Team::getEventId, eventId)
                .eq(Team::getTrackId, registration.getTrackId())
                .eq(Team::getStatus, TeamStatus.ACTIVE)
                .lt(Team::getSize, maxSize(event))
                .like(Team::getName, dto.getName().trim())
                .orderByDesc(Team::getUpdateTime);
        return teamMapper.selectPage(new Page<>(param.getPage(), param.getSize()), wrapper)
                .convert(this::teamBriefVO);
    }

    private TeamBriefVO teamBriefVO(Team team) {
        TeamBriefVO vo = new TeamBriefVO();
        BeanUtils.copyProperties(team, vo);
        Track track = trackMapper.selectById(team.getTrackId());
        User leader = userMapper.selectById(team.getLeaderId());
        vo.setTrackName(track == null ? null : track.getName());
        vo.setLeaderName(leader == null ? null : leader.getName());
        Event event = eventMapper.selectById(team.getEventId());
        vo.setMaxSize(event == null ? null : maxSize(event));
        return vo;
    }
}
