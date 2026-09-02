package org.hackathon.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.QueryUserSearchDTO;
import org.hackathon.data.dto.UpdateContactDTO;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.UserSearchVO;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.ExUserMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void updatesProfileWhenContactDetailsAreUnchanged() {
        UserMapper userMapper = mock(UserMapper.class);
        ExUserMapper exUserMapper = mock(ExUserMapper.class);
        AuthorityMapper authorityMapper = mock(AuthorityMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService service = new UserService(userMapper, exUserMapper, authorityMapper, studentMapper);
        User user = new User(101, "张三", null, true, "13800138000", "zhangsan@example.com", null, null);
        when(userMapper.selectById(101)).thenReturn(user);

        service.updateContact(new UpdateContactDTO("13800138000", "zhangsan@example.com"), 101);

        verify(userMapper, never()).selectCount(any());
        verify(userMapper).updateById(user);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void searchesUsersByUserNameWithoutReturningContactDetails() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        UserMapper userMapper = mock(UserMapper.class);
        ExUserMapper exUserMapper = mock(ExUserMapper.class);
        AuthorityMapper authorityMapper = mock(AuthorityMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserService service = new UserService(userMapper, exUserMapper, authorityMapper, studentMapper);

        User studentUser = new User(101, "张三", null, true, null, null, null, null);
        User externalUser = new User(102, "张三老师", null, false, null, null, null, null);
        Student student = new Student(101, "candidate", "中心校区", "软件工程",
                null, "Java,Spring", null, null);
        Page<User> selected = new Page<>(1, 10, 2);
        selected.setRecords(List.of(studentUser, externalUser));
        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(selected);
        when(studentMapper.selectByIds(List.of(101))).thenReturn(List.of(student));

        QueryUserSearchDTO query = new QueryUserSearchDTO();
        query.setUserName(" 张三 ");
        IPage<UserSearchVO> result = service.searchUserPage(query, new PageParamDTO());

        assertEquals(2, result.getTotal());
        assertEquals(new UserSearchVO(101, "张三", true, "中心校区", "软件工程",
                List.of("Java", "Spring")), result.getRecords().get(0));
        assertEquals(new UserSearchVO(102, "张三老师", false, null, null, List.of()),
                result.getRecords().get(1));
        verify(studentMapper).selectByIds(List.of(101));

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getCustomSqlSegment().contains("name LIKE"));
    }
}
