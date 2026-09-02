package org.hackathon.service;

import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.Event;
import org.hackathon.data.vo.AuthorityEventVO;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorityServiceTest {

    @Test
    void returnsOnlyCurrentUsersAdminEvents() {
        AuthorityMapper authorityMapper = mock(AuthorityMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        EventMapper eventMapper = mock(EventMapper.class);
        AuthorityService service = new AuthorityService(authorityMapper, userMapper, eventMapper);
        LocalDateTime now = LocalDateTime.now();

        when(authorityMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                new Authority(1, 7, AuthorityEnum.ADMIN, 101, now.minusHours(1)),
                new Authority(2, 7, AuthorityEnum.JUDGE, 102, now),
                new Authority(3, 7, AuthorityEnum.ADMIN, 103, now)
        )));
        when(eventMapper.selectByIds(List.of(103, 101))).thenReturn(List.of(
                event(101, "创新赛", now), event(103, "创意赛", now)
        ));

        List<AuthorityEventVO> result = service.getAdminEventListByUser(7);

        assertEquals(2, result.size());
        assertEquals(AuthorityEnum.ADMIN, result.get(0).getType());
        assertEquals(103, result.get(0).getEventId());
        assertEquals("创意赛", result.get(0).getEventName());
        assertEquals(101, result.get(1).getEventId());
        verify(authorityMapper).selectList(any());
        verify(eventMapper).selectByIds(List.of(103, 101));
    }

    private Event event(Integer eventId, String name, LocalDateTime now) {
        return new Event(eventId, name, now.minusDays(3), now.minusDays(2), now.minusDays(1),
                now.plusDays(1), null, null, null, 1, 5, 0, now, now);
    }
}
