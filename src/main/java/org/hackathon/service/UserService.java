package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateUserDTO;
import org.hackathon.data.dto.PasswordDTO;
import org.hackathon.data.dto.UpdateUserDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.GetUserVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;

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

    public GetUserVO getUserInfo(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return new GetUserVO(user.getPhone(), user.getEmail());
    }

    public void updateUserInfo(UpdateUserDTO dto, Integer userId) {
        User user = userMapper.selectById(userId);
        boolean update = false;
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())) {
            verifyPhone(dto.getPhone());
            user.setPhone(dto.getPhone());
            update = true;
        }
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            verifyEmail(dto.getEmail());
            user.setEmail(dto.getEmail());
            update = true;
        }
        if (update) user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void updatePassword(PasswordDTO dto, Integer userId) {
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

    public void createNonstudentUser(CreateUserDTO dto) {
        verifyPhone(dto.getPhone());
        verifyEmail(dto.getEmail());
        User user = new User(
                null, dto.getName(), BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                false, dto.getPhone(), dto.getEmail(), LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
    }
}
