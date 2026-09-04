package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateStudentDTO;
import org.hackathon.data.dto.UpdateStudentDTO;
import org.hackathon.data.dto.UpdateContactDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.ExUser;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.CreateStudentVO;
import org.hackathon.data.vo.StudentInfoVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.ExUserMapper;
import org.hackathon.mapper.StudentMapper;
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
    private final StudentTagService tagService;
    private final UserService userService;
    private final AuthorityService authorityService;
    private final ExUserMapper exUserMapper;



    private void verifyTags(List<String> tags) {
        if (tags == null) {
            tags = List.of();
        }
        List<String> ava = tagService.getAvailableTags();
        for (String tag : tags) {
            if (!ava.contains(tag)) {
                throw new BusinessException(ResultCode.TAG_UNAVAILABLE);
            }
        }
    }

    @Transactional
    public CreateStudentVO createStudent(CreateStudentDTO dto) {
        verifyTags(dto.getTags());
        LocalJwt jwt = localJwtUtils.parseToken(dto.getToken(), LocalJwt.Type.REGISTER);
        if (jwt.getCasId() == null || jwt.getCasId().length() > 12) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号不能超过12位");
        }
        long count = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().eq(Student::getCasId, jwt.getCasId())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.ALREADY_ENROLLED);
        }
        User up= userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        User ue = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        User user;
        boolean existed = false;
        if (up != null && up.equals(ue)) {
            user = up;
            ExUser exUser = exUserMapper.selectById(user.getUserId());
            if (exUser == null || !exUser.getOnCampus()) {
                throw new BusinessException(ResultCode.NOT_ON_CAMPUS);
            }
            user.setName(jwt.getName());
            user.setStudentFlag(true);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            exUserMapper.deleteById(user.getUserId());
            existed = true;
        } else {
            if (up != null) {
                throw new BusinessException(ResultCode.PHONE_CONFLICT);
            }
            if (ue != null) {
                throw new BusinessException(ResultCode.EMAIL_CONFLICT);
            }
            user = new User(
                    null, jwt.getName(),
                    dto.getPassword() == null ? null : BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(10)),
                    true, dto.getPhone(), dto.getEmail(),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            userMapper.insert(user);
        }
        Student student = new Student(
                user.getUserId(), jwt.getCasId(), dto.getCampus(), dto.getMajor(), "",
                String.join(",", dto.getTags()), LocalDateTime.now(), LocalDateTime.now()
        );
        studentMapper.insert(student);
        jwt.setUserId(user.getUserId());
        return new CreateStudentVO(
                localJwtUtils.generateToken(jwt, LocalJwt.Type.ACCESS), jwt.getName(),
                true, jwt.getCasId(), existed,
                existed ? authorityService.getAuthorityListByUser(user.getUserId()) : List.of()
        );
    }

    public StudentInfoVO getStudent(Integer userId) {
        User user = userMapper.selectById(userId);
        Student student = studentMapper.selectById(userId);
        if (user == null || student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        }
        return new StudentInfoVO(
                user.getPhone(), user.getEmail(), student.getCampus(), student.getMajor(),
                student.getIntroduction(), student.getTagsAsList()
        );
    }

    @Transactional
    public void updateStudent(UpdateStudentDTO dto, Integer userId) {
        userService.updateContact(new UpdateContactDTO(dto.getPhone(), dto.getEmail()), userId);
        Student student = studentMapper.selectById(userId);
        if (student == null) {
            throw new BusinessException(ResultCode.STUDENT_NOT_EXIST);
        }
        student.setCampus(dto.getCampus());
        student.setMajor(dto.getMajor());
        student.setIntroduction(dto.getIntroduction());
        verifyTags(dto.getTags());
        String tags = String.join(",", dto.getTags());
        student.setTags(tags);
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.updateById(student);
    }

}
