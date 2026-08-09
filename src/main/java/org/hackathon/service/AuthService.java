package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.data.dto.LoginDTO;
import org.hackathon.data.dto.RegisterStudentDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.data.vo.Result;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LocalJwtUtils localJwtUtils;
    private final StudentMapper studentMapper;

    public Integer examineStudent(String casID) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, casID)
        );
        if (user == null) {
            return null;
        }
        return user.getId();
    }

    private void verifyPhone(String phone) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new BusinessException(ResultCode.PHONE_CONFLICT);
        }
    }

    private void verifyEmail(String email) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ResultCode.EMAIL_CONFLICT);
        }
    }

    @Transactional
    public ResponseEntity<Result<LoginVO>> studentRegister(RegisterStudentDTO dto) {
        LocalJwt jwt = localJwtUtils.parseToken(dto.getToken());
        if (examineStudent(jwt.getCasID()) != null) {
            throw new BusinessException(ResultCode.ALREADY_REGISTERED);
        }
        verifyPhone(dto.getPhone());
        verifyEmail(dto.getEmail());
        User user = new User(
                null, jwt.getCasID(),
                dto.getPassword() == null ? null : BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                true, dto.getPhone(), dto.getEmail(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
        Student student = new Student(
                user.getId(), jwt.getName(), dto.getCampus(), dto.getMajor(), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        studentMapper.insert(student);
        jwt.setId(user.getId());
        return Result.success(
                new LoginVO(localJwtUtils.generateToken(jwt, false), jwt.getName(), true,
                        jwt.getCasID()), "注册成功"
        );
    }

    public ResponseEntity<Result<LoginVO>> exchangeToken(String temp) {
        LocalJwt jwt = localJwtUtils.parseToken(temp);
        if (jwt.getCasID().equals(String.valueOf(-1))) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        return Result.success(
                new LoginVO(localJwtUtils.generateToken(jwt, false), jwt.getName(), true,
                        jwt.getCasID()), "兑换成功"
        );
    }

    public ResponseEntity<Result<LoginVO>> localLogin(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (dto.getTerm().matches("^\\d{12}$")) {
            wrapper.eq(User::getUsername, dto.getTerm());
        } else if (dto.getTerm().matches("^1[3-9]\\d{9}$")) {
            wrapper.eq(User::getPhone, dto.getTerm());
        } else {
            wrapper.eq(User::getEmail, dto.getTerm());
        }
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        }
        LocalJwt jwt = new LocalJwt();
        jwt.setId(user.getId());
        jwt.setIsStudent(user.getIsStudent());
        if (user.getIsStudent()) {
            jwt.setName(studentMapper.selectById(user.getId()).getName());
            jwt.setCasID(user.getUsername());
        } else {
            jwt.setName(user.getUsername());
            jwt.setCasID("");
        }
        String token = localJwtUtils.generateToken(jwt, false);
        return Result.success(
                new LoginVO(token, jwt.getName(), jwt.getIsStudent(), jwt.getCasID()), "登录成功"
        );
    }
}
