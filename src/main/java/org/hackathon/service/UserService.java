package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateExUserDTO;
import org.hackathon.data.dto.UpdateExUserDTO;
import org.hackathon.data.dto.UpdatePasswordDTO;
import org.hackathon.data.dto.UpdateContactDTO;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.ExUser;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.*;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.ExUserMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final ExUserMapper exUserMapper;
    private final AuthorityMapper authorityMapper;
    private final StudentMapper studentMapper;

    public void verifyPhone(String phone) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new BusinessException(ResultCode.PHONE_CONFLICT);
        }
    }

    public void verifyEmail(String email) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ResultCode.EMAIL_CONFLICT);
        }
    }

    public ExUserInfoVO getExUser(Integer userId) {
        User user = userMapper.selectById(userId);
        ExUser exUser = exUserMapper.selectById(userId);
        if (user == null || exUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return new ExUserInfoVO(user.getPhone(), user.getEmail(),
                exUser.getOnCampus(), exUser.getOrganization());
    }

    public UserInfoVO getUserById(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(userId);
        vo.setStudentFlag(user.getStudentFlag());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        if (user.getStudentFlag()) {
            Student student = studentMapper.selectById(userId);
            if (student == null) throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
            StudentBriefVO brief = new StudentBriefVO(
                    student.getCampus(), student.getMajor(), student.getIntroduction(),
                    student.getTagsAsList()
            );
            vo.setName(student.getName());
            vo.setStudent(brief);
        } else {
            ExUser exUser = exUserMapper.selectById(userId);
            if (exUser == null) throw new BusinessException(ResultCode.EX_USER_NOT_EXIST);
            ExUserBriefVO brief = new ExUserBriefVO(
                    exUser.getOnCampus(), exUser.getOrganization()
            );
            vo.setName(user.getUsername());
            vo.setExUser(brief);
        }
        return vo;
    }

    public void updateContact(UpdateContactDTO dto, Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        verifyPhone(dto.getPhone());
        user.setPhone(dto.getPhone());
        verifyEmail(dto.getEmail());
        user.setEmail(dto.getEmail());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Transactional
    public void updateExUser(UpdateExUserDTO dto, Integer userId) {
        updateContact(new UpdateContactDTO(dto.getPhone(), dto.getEmail()), userId);
        ExUser exUser = exUserMapper.selectById(userId);
        if (exUser == null) {
            throw new BusinessException(ResultCode.EX_USER_NOT_EXIST);
        }
        exUser.setOrganization(dto.getOrganization());
        exUser.setUpdateTime(LocalDateTime.now());
        exUserMapper.updateById(exUser);
    }

    public void updatePassword(UpdatePasswordDTO dto, Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (user.getPassword() == null && !dto.getOldPassword().isEmpty()) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        } else if (user.getPassword() != null && !BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_INCORRECT);
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt(10)));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    //@Transactional
    private UserIdVO createExUser(CreateExUserDTO dto) {
        verifyPhone(dto.getPhone());
        verifyEmail(dto.getEmail());
        User user = new User(
                null, dto.getName(), BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                false, dto.getPhone(), dto.getEmail(), LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
        System.out.println("iunnim");
        exUserMapper.insert(new ExUser(user.getUserId(), dto.getOnCampus(), dto.getOrganization(),
                LocalDateTime.now(), LocalDateTime.now()));
        return new UserIdVO(user.getUserId());
    }

    @Transactional
    public UserIdVO createExAdmin(CreateExUserDTO dto, Integer eventId) {
        UserIdVO vo = createExUser(dto);
        Authority authority = new Authority(null, vo.getUserId(), AuthorityEnum.ADMIN,
                eventId, LocalDateTime.now());
        authorityMapper.insert(authority);
        return vo;
    }

    @Transactional
    public UserIdVO createExJudge(CreateExUserDTO dto, Integer eventId) {
        UserIdVO vo = createExUser(dto);
        Authority authority = new Authority(null, vo.getUserId(), AuthorityEnum.JUDGE,
                eventId, LocalDateTime.now());
        authorityMapper.insert(authority);
        return vo;
    }
}
