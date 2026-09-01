package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.hackathon.config.GlobalProperties;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.QueryInviteCandidateDTO;
import org.hackathon.data.enums.TeamEnum;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.InviteCandidateVO;
import org.hackathon.mapper.*;
import org.hackathon.security.Context;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void searchesOnlyInvitableStudentsByUserName() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
        TeamMapper teamMapper = mock(TeamMapper.class);
        EventMapper eventMapper = mock(EventMapper.class);
        TrackMapper trackMapper = mock(TrackMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        TeamRecruitmentMapper recruitmentMapper = mock(TeamRecruitmentMapper.class);
        TeamApplicationMapper applicationMapper = mock(TeamApplicationMapper.class);
        TeamInvitationMapper invitationMapper = mock(TeamInvitationMapper.class);
        StudentRecommendationMapper recommendationMapper = mock(StudentRecommendationMapper.class);
        NotificationService notificationService = mock(NotificationService.class);
        GlobalProperties properties = new GlobalProperties(5, null, null, null, null);
        TeamService service = new TeamService(registrationMapper, teamMapper, eventMapper, trackMapper,
                userMapper, studentMapper, recruitmentMapper, applicationMapper, invitationMapper,
                recommendationMapper, notificationService, properties);

        Team team = new Team();
        team.setTeamId(21);
        team.setEventId(3);
        team.setTrackId(8);
        team.setLeaderId(100);
        team.setSize(1);
        team.setType(TeamEnum.SAME_MAJOR);
        Event event = new Event();
        event.setEventId(3);
        event.setLiveBeg(LocalDateTime.now().plusDays(1));
        Student leader = new Student(100, "leader", "中心校区", "软件工程", null, null, null, null);
        Student candidate = new Student(101, "candidate", "中心校区", "软件工程",
                null, "Java,Spring", null, null);
        User candidateUser = new User();
        candidateUser.setUserId(101);
        candidateUser.setName("张三");
        Page<User> selected = new Page<>(1, 10, 1);
        selected.setRecords(List.of(candidateUser));

        Context context = mock(Context.class);
        when(context.team()).thenReturn(team);
        when(eventMapper.selectById(3)).thenReturn(event);
        when(studentMapper.selectById(100)).thenReturn(leader);
        when(studentMapper.selectByIds(List.of(101))).thenReturn(List.of(candidate));
        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(selected);

        QueryInviteCandidateDTO query = new QueryInviteCandidateDTO();
        query.setUserName(" 张三 ");
        PageParamDTO page = new PageParamDTO();
        IPage<InviteCandidateVO> result = service.inviteCandidatePage(query, page, context);

        assertEquals(1, result.getTotal());
        assertEquals(new InviteCandidateVO(101, "张三", "中心校区", "软件工程",
                List.of("Java", "Spring")), result.getRecords().getFirst());

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getCustomSqlSegment();
        assertAll(
                () -> assertTrue(sql.contains("name LIKE")),
                () -> assertTrue(sql.contains("registration r")),
                () -> assertTrue(sql.contains("r.team_id IS NULL")),
                () -> assertTrue(sql.contains("s.major")),
                () -> assertTrue(sql.contains("s.campus")),
                () -> assertTrue(sql.contains("team_invitation ti"))
        );
    }
}
