package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.config.GlobalProperties;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.CreateTeamDTO;
import org.hackathon.data.dto.UpdateTeamDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.enums.TeamStatus;
import org.hackathon.data.po.*;
import org.hackathon.data.vo.TeamInfoVO;
import org.hackathon.data.vo.TeamMemberVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.*;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final RegistrationMapper regMapper;
    private final TeamMapper teamMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final GlobalProperties globalProperties;

    @Transactional
    public Integer createTeam(CreateTeamDTO dto, EventContext context, Integer userId) {
        Event event = context.getEvent();
        Integer trackId = context.getTrack().getTrackId();
        if (event.getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
        Registration registration = regMapper.selectOne(
                new LambdaQueryWrapper<Registration>().eq(Registration::getUserId, userId)
                        .eq(Registration::getEventId, event.getEventId())
        );
        if (registration == null || !registration.getTrackId().equals(trackId)) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        if (registration.getTeamId() != null) {
            throw new BusinessException(ResultCode.ALREADY_TEAMED);
        }
        Team team = new Team(null, dto.getName(), event.getEventId(), trackId, userId,
                1, dto.getIntroduction(), dto.getType(), TeamStatus.ACTIVE, null,
                1, LocalDateTime.now(), LocalDateTime.now());
        try {
            teamMapper.insert(team);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.TEAM_NAME_CONFLICT);
        }
        registration.setTeamId(team.getTeamId());
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        return registration.getTeamId();
    }

    private List<Integer> getMemberIds(Integer teamId) {
        return regMapper.selectList(
                new LambdaQueryWrapper<Registration>().eq(Registration::getTeamId, teamId)
                        .select(Registration::getUserId)
        ).stream().map(Registration::getUserId).toList();
    }

    private List<TeamMemberVO> getTeamMembers(Integer teamId, Integer leaderId) {
        List<Integer> ids = getMemberIds(teamId);
        Map<Integer, String> map = userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
        return studentMapper.selectByIds(ids).stream().map(po -> new TeamMemberVO(
                po.getUserId(), map.get(po.getUserId()), po.getCasId(), po.getCampus(), po.getMajor(),
                po.getTagsAsList(), po.getUserId().equals(leaderId)
        )).toList();
    }

    public TeamInfoVO getTeam(Team team) {
        TeamInfoVO vo = new TeamInfoVO();
        BeanUtils.copyProperties(team, vo);
        vo.setEventName(eventMapper.selectById(team.getEventId()).getName());
        vo.setTrackName(trackMapper.selectById(team.getTrackId()).getName());
        vo.setMembers(getTeamMembers(team.getTeamId(), team.getLeaderId()));
        return vo;
    }

    public void updateTeam(UpdateTeamDTO dto, Team team) {
        if (eventMapper.selectById(team.getEventId()).getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
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
            if (teamMapper.updateById(team) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.TEAM_NAME_CONFLICT);
        }
    }

    @Transactional
    public void joinTeam(Team team, Integer userId) {
        if (eventMapper.selectById(team.getEventId()).getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
        if (team.getSize() >= globalProperties.teamCapacity()) {
            throw new BusinessException(ResultCode.TEAM_ALREADY_FULL);
        }
        Student leader = studentMapper.selectById(team.getLeaderId());
        Student student = studentMapper.selectById(userId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        }
        if (team.getType() == TeamEnum.SAME_MAJOR && !student.getMajor().equals(leader.getMajor())) {
            throw new BusinessException(ResultCode.TEAM_TYPE_CONFLICT);
        }
        if (team.getType() != TeamEnum.CROSS_CAMPUS && !student.getCampus().equals(leader.getCampus())) {
            throw new BusinessException(ResultCode.TEAM_TYPE_CONFLICT);
        }
        team.setSize(team.getSize() + 1);
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        Registration registration = regMapper.selectOne(
                new LambdaQueryWrapper<Registration>().eq(Registration::getUserId, userId)
        .eq(Registration::getEventId, team.getEventId()));
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        if (registration.getTeamId() != null) {
            throw new BusinessException(ResultCode.ALREADY_TEAMED);
        }
        registration.setTeamId(team.getTeamId());
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    @Transactional
    public void leaveTeam(Team team, Integer userId) {
        if (eventMapper.selectById(team.getEventId()).getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
        if (team.getLeaderId().equals(userId)) {
            throw new BusinessException(ResultCode.LEADER_LEAVING);
        }
        Registration registration = regMapper.selectOne(
                new LambdaQueryWrapper<Registration>().eq(Registration::getUserId, userId)
                        .eq(Registration::getTeamId, team.getTeamId())
        );
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_TEAM_MEMBER);
        }
        team.setSize(team.getSize() - 1);
        team.setUpdateTime(LocalDateTime.now());
        if (teamMapper.updateById(team) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        registration.setTeamId(null);
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    @Transactional
    public void deleteTeam(Team team) {
        if (eventMapper.selectById(team.getEventId()).getLiveBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_LIVE);
        }
        regMapper.update(new LambdaUpdateWrapper<Registration>()
                .eq(Registration::getTeamId, team.getTeamId())
                .set(Registration::getTeamId, null));
        teamMapper.deleteById(team);
    }
}
