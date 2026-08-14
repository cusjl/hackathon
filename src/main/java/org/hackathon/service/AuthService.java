package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.po.Student;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.data.dto.LoginDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LocalJwtUtils localJwtUtils;
    private final StudentMapper studentMapper;
    private final AuthorityService authorityService;

    public Integer examineStudent(String casID) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getCasId, casID)
        );
        if (student == null) {
            return null;
        }
        return student.getUserId();
    }

    public LoginVO exchangeToken(String temp) {
        LocalJwt jwt = localJwtUtils.parseToken(temp);
        if (jwt.getUserId() == -1) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        return new LoginVO(
                localJwtUtils.generateToken(jwt, false), jwt.getName(), true,
                        jwt.getCasId(), authorityService.getAuthorityListByUser(jwt.getUserId())
        );
    }

    public LoginVO localLogin(LoginDTO dto) {
        User user;
        if (dto.getTerm().matches("^\\d{12}$")) {
            Student student = studentMapper.selectOne(
                    new LambdaQueryWrapper<Student>().eq(Student::getCasId, dto.getTerm())
                            .select(Student::getUserId)
            );
            if (student == null) {
                throw new BusinessException(ResultCode.NOT_REGISTERED);
            }
            user = userMapper.selectById(student.getUserId());
        } else {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (dto.getTerm().matches("^1[3-9]\\d{9}$")) {
                wrapper.eq(User::getPhone, dto.getTerm());
            } else {
                wrapper.eq(User::getEmail, dto.getTerm());
            }
            user = userMapper.selectOne(wrapper);
        }
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (user.getPassword() == null) {
            throw new BusinessException(ResultCode.PASSWORD_UNSET);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        }
        LocalJwt jwt = new LocalJwt();
        jwt.setUserId(user.getUserId());
        jwt.setStudentFlag(user.getStudentFlag());
        jwt.setName(user.getName());
        jwt.setCasId(user.getStudentFlag() ? dto.getTerm() : null);
        String token = localJwtUtils.generateToken(jwt, false);
        return new LoginVO(
                token, jwt.getName(), jwt.getStudentFlag(), jwt.getCasId(),
                authorityService.getAuthorityListByUser(jwt.getUserId())
        );
    }
}
