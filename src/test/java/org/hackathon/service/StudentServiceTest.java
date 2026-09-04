package org.hackathon.service;

import org.hackathon.data.dto.CreateStudentDTO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.ExUserMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudentServiceTest {

    @Test
    void rejectsAnOverlongCasIdFromTheRegistrationTokenBeforeDatabaseAccess() {
        LocalJwtUtils jwtUtils = mock(LocalJwtUtils.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        StudentTagService tagService = mock(StudentTagService.class);
        StudentService service = new StudentService(jwtUtils, mock(UserMapper.class), studentMapper, tagService,
                mock(UserService.class), mock(AuthorityService.class), mock(ExUserMapper.class));
        CreateStudentDTO dto = new CreateStudentDTO();
        dto.setToken("register-token");
        dto.setTags(List.of());
        when(tagService.getAvailableTags()).thenReturn(List.of());
        when(jwtUtils.parseToken("register-token", LocalJwt.Type.REGISTER))
                .thenReturn(new LocalJwt(null, "学生", true, "2026000000001"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createStudent(dto));

        assertEquals(4000, exception.getCode());
        assertEquals("学号不能超过12位", exception.getMsg());
        verify(studentMapper, never()).selectCount(any());
    }
}
