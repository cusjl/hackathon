package org.hackathon.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.hackathon.config.GlobalProperties;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.QueryJoinableTeamDTO;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.enums.TeamJoinUnavailableReason;
import org.hackathon.data.enums.TeamStatus;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Registration;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.TeamBriefVO;
import org.hackathon.mapper.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void preflightsCollaborationRulesAndAllowsCrossCampusMembers() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Team.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Registration.class);

        RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
        TeamMapper teamMapper = mock(TeamMapper.class);
        EventMapper eventMapper = mock(EventMapper.class);
        TrackMapper trackMapper = mock(TrackMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        TeamService service = new TeamService(registrationMapper, teamMapper, eventMapper, trackMapper,
                userMapper, studentMapper, mock(TeamRecruitmentMapper.class), mock(TeamApplicationMapper.class),
                mock(TeamInvitationMapper.class), mock(StudentRecommendationMapper.class),
                mock(NotificationService.class), new GlobalProperties(5, null, null, null, null));

        Event event = new Event(7, "创新赛", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), null, null, null,
                1, 5, 1, LocalDateTime.now(), LocalDateTime.now());
        Team sameMajorTeam = team(11, TeamEnum.SAME_MAJOR);
        Team crossCampusTeam = team(12, TeamEnum.CROSS_CAMPUS);
        Student applicant = student(1, "青岛校区", "人工智能");
        Student existingMember = student(2, "中心校区", "软件工程");
        Page<Team> page = new Page<>(1, 10, 2);
        page.setRecords(List.of(sameMajorTeam, crossCampusTeam));

        when(eventMapper.selectById(7)).thenReturn(event);
        when(registrationMapper.selectOne(any())).thenReturn(new Registration(1, 1, 7, 3, null, 1, null, null));
        when(studentMapper.selectById(1)).thenReturn(applicant);
        when(teamMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        org.hackathon.data.po.Track track = new org.hackathon.data.po.Track();
        track.setTrackId(3);
        track.setEventId(7);
        track.setName("主赛道");
        when(trackMapper.selectById(3)).thenReturn(track);
        when(userMapper.selectById(2)).thenReturn(new User(2, "队长", null, true, null, null, null, null));
        when(registrationMapper.selectList(any())).thenReturn(List.of(
                new Registration(2, 2, 7, 3, 11, 1, null, null)
        ));
        when(studentMapper.selectByIds(List.of(2))).thenReturn(List.of(existingMember));

        QueryJoinableTeamDTO query = new QueryJoinableTeamDTO();
        query.setName("队");
        IPage<TeamBriefVO> result = service.joinableTeamPage(7, query, new PageParamDTO(), 1);

        assertEquals(false, result.getRecords().get(0).getJoinable());
        assertEquals(TeamJoinUnavailableReason.MAJOR_MISMATCH, result.getRecords().get(0).getUnavailableReason());
        assertEquals(true, result.getRecords().get(1).getJoinable());
        assertEquals(null, result.getRecords().get(1).getUnavailableReason());
    }

    private Team team(int id, TeamEnum type) {
        Team team = new Team();
        team.setTeamId(id);
        team.setName("队伍" + id);
        team.setEventId(7);
        team.setTrackId(3);
        team.setLeaderId(2);
        team.setSize(1);
        team.setType(type);
        team.setStatus(TeamStatus.ACTIVE);
        team.setUpdateTime(LocalDateTime.now());
        return team;
    }

    private Student student(int userId, String campus, String major) {
        return new Student(userId, "2026000000" + userId, campus, major, null, null, null, null);
    }
}
