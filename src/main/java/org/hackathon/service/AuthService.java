package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.Event;
import org.hackathon.data.vo.AuthorityVO;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final LocalJwtUtils localJwtUtils;
    private final StudentMapper studentMapper;
    private final AuthorityMapper authorityMapper;
    private final EventMapper eventMapper;

    public Integer examineStudent(String casID) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, casID)
        );
        if (user == null) {
            return null;
        }
        return user.getUserId();
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

    private List<AuthorityVO> getAuthorityVOList(Integer userId) {
        List<Authority> list = authorityMapper.selectList(
                new LambdaQueryWrapper<Authority>().eq(Authority::getUserId, userId)
        );
        list.sort(
                Comparator.comparingInt(Authority::getTypeValue)
                .thenComparing(Comparator.comparing(Authority::getCreateTime).reversed())
        );
        List<Integer> ids = list.stream().map(Authority::getEventId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Integer, String> map = ids.isEmpty() ? new HashMap<>()
                : eventMapper.selectByIds(ids).stream()
                  .collect(Collectors.toMap(Event::getEventId, Event::getName));
        return list.stream().map(po -> {
            AuthorityVO vo = new AuthorityVO();
            vo.setType(po.getType().getDesc());
            vo.setEventId(po.getEventId());
            if (po.getEventId() != null) {
                vo.setEventId(po.getEventId());
                vo.setEventName(map.get(po.getEventId()));
            }
            return vo;
        }).toList();
    }

    @Transactional
    public ResponseEntity<Result<LoginVO>> studentRegister(RegisterStudentDTO dto) {
        LocalJwt jwt = localJwtUtils.parseToken(dto.getToken());
        if (examineStudent(jwt.getCasId()) != null) {
            throw new BusinessException(ResultCode.ALREADY_REGISTERED);
        }
        verifyPhone(dto.getPhone());
        verifyEmail(dto.getEmail());
        User user = new User(
                null, jwt.getCasId(),
                dto.getPassword() == null ? null : BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                true, dto.getPhone(), dto.getEmail(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
        Student student = new Student(
                user.getUserId(), jwt.getName(), dto.getCampus(), dto.getMajor(), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        studentMapper.insert(student);
        jwt.setUserId(user.getUserId());
        return Result.success(
                new LoginVO(localJwtUtils.generateToken(jwt, false), jwt.getName(), true,
                        jwt.getCasId(), List.of()), "注册成功"
        );
    }

    public ResponseEntity<Result<LoginVO>> exchangeToken(String temp) {
        LocalJwt jwt = localJwtUtils.parseToken(temp);
        if (jwt.getCasId().equals(String.valueOf(-1))) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        return Result.success(
                new LoginVO(localJwtUtils.generateToken(jwt, false), jwt.getName(), true,
                        jwt.getCasId(), getAuthorityVOList(jwt.getUserId())), "兑换成功"
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
        jwt.setUserId(user.getUserId());
        jwt.setIsStudent(user.getIsStudent());
        if (user.getIsStudent()) {
            jwt.setName(studentMapper.selectById(user.getUserId()).getName());
            jwt.setCasId(user.getUsername());
        } else {
            jwt.setName(user.getUsername());
            jwt.setCasId("");
        }
        String token = localJwtUtils.generateToken(jwt, false);
        return Result.success(
                new LoginVO(token, jwt.getName(), jwt.getIsStudent(), jwt.getCasId(),
                        getAuthorityVOList(jwt.getUserId())), "登录成功"
        );
    }
}
