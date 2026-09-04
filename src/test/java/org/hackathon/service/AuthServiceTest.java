package org.hackathon.service;

import org.hackathon.data.dto.LoginDTO;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void emailPasswordLoginReturnsStableUserIdAndActualCasId() {
        UserMapper userMapper = mock(UserMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        LocalJwtUtils jwtUtils = mock(LocalJwtUtils.class);
        AuthorityService authorityService = mock(AuthorityService.class);
        AuthService service = new AuthService(userMapper, jwtUtils, studentMapper, authorityService);
        User user = new User(42, "队长", BCrypt.hashpw("correct-password", BCrypt.gensalt()), true,
                "13800138000", "leader@example.com", null, null);
        Student student = new Student(42, "202600000042", "中心校区", "软件工程", null, null, null, null);
        LoginDTO request = new LoginDTO();
        request.setTerm("leader@example.com");
        request.setPassword("correct-password");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(studentMapper.selectById(42)).thenReturn(student);
        when(jwtUtils.generateToken(any(LocalJwt.class), eq(LocalJwt.Type.ACCESS))).thenReturn("access-token");
        when(authorityService.getAuthorityListByUser(42)).thenReturn(List.of());

        LoginVO response = service.localLogin(request);

        assertEquals(42, response.getUserId());
        assertEquals("202600000042", response.getCasId());
        assertEquals("leader@example.com", response.getEmail());
    }
}
