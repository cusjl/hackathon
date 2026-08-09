package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.GenerateJwtDTO;
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
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, casID);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }
        return user.getId();
    }

    @Transactional
    public ResponseEntity<Result<LoginVO>> studentRegister(RegisterStudentDTO dto) {
        GenerateJwtDTO info = localJwtUtils.parseJwt(dto.getToken());
        if (examineStudent(info.getCasID()) != null) {
            throw new BusinessException(ResultCode.ALREADY_REGISTERED);
        }
        User user = new User(
                null, info.getCasID(),
                dto.getPassword() == null ? null : BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                true, dto.getPhone(), dto.getEmail(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
        Student student = new Student(
                user.getId(), info.getName(), dto.getCampus(), dto.getMajor(), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        studentMapper.insert(student);
        info.setId(user.getId());
        return Result.success(
                new LoginVO(localJwtUtils.generateJwt(info), info.getName(), true, info.getCasID()),
                "注册成功"
        );
    }

}
