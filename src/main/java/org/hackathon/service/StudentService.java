package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateStudentDTO;
import org.hackathon.data.dto.UpdateStudentDTO;
import org.hackathon.data.dto.UpdateUserDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.StudentTag;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.data.vo.GetStudentVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.StudentTagMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final LocalJwtUtils localJwtUtils;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final StudentTagMapper tagMapper;
    private final UserService userService;

    public List<String> getAvailableTags() {
        List<StudentTag> list = tagMapper.selectList(null);
        return list.stream().map(StudentTag::getName).toList();
    }

    private void verifyTags(List<String> tags) {
        if (tags == null) {
            tags = List.of();
        }
        List<String> ava = getAvailableTags();
        for (String tag : tags) {
            if (!ava.contains(tag)) {
                throw new BusinessException(ResultCode.TAG_UNAVAILABLE);
            }
        }
    }

    @Transactional
    public LoginVO register(CreateStudentDTO dto) {
        LocalJwt jwt = localJwtUtils.parseToken(dto.getToken());
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, jwt.getCasId())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_REGISTERED);
        }
        userService.verifyPhone(dto.getPhone());
        userService.verifyEmail(dto.getEmail());
        verifyTags(dto.getTags());
        User user = new User(
                null, jwt.getCasId(),
                dto.getPassword() == null ? null : BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                true, dto.getPhone(), dto.getEmail(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
        Student student = new Student(
                user.getUserId(), jwt.getName(), dto.getCampus(), dto.getMajor(), "",
                String.join(",",dto.getTags()), LocalDateTime.now(), LocalDateTime.now()
        );
        studentMapper.insert(student);
        jwt.setUserId(user.getUserId());
        return new LoginVO(
                localJwtUtils.generateToken(jwt, false), jwt.getName(),
                true, jwt.getCasId(), List.of()
        );
    }

    public GetStudentVO getInfo(Integer userId) {
        User user = userMapper.selectById(userId);
        Student student = studentMapper.selectById(userId);
        if (user == null || student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        }
        return new GetStudentVO(
                user.getPhone(), user.getEmail(), student.getCampus(), student.getMajor(),
                student.getIntroduction(), student.getTagsAsList()
        );
    }

    @Transactional
    public void updateInfo(UpdateStudentDTO dto, Integer userId) {
        userService.updateUserInfo(new UpdateUserDTO(dto.getPhone(), dto.getEmail()), userId);
        Student student = studentMapper.selectById(userId);
        boolean update = false;
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        }
        if (dto.getCampus() != null && !dto.getCampus().equals(student.getCampus())) {
            student.setCampus(dto.getCampus());
            update = true;
        }
        if (dto.getMajor() != null && !dto.getMajor().equals(student.getMajor())) {
            student.setMajor(dto.getMajor());
            update = true;
        }
        if (dto.getIntroduction() != null && !dto.getIntroduction().equals(student.getIntroduction())) {
            student.setIntroduction(dto.getIntroduction());
            update = true;
        }
        if (dto.getTags() != null) {
            verifyTags(dto.getTags());
            String tags = String.join(",", dto.getTags());
            if (!tags.equals(student.getTags())) {
                student.setTags(tags);
                update = true;
            }
        }
        if (update) {
            student.setUpdateTime(LocalDateTime.now());
            studentMapper.updateById(student);
        }
    }

}
