package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.Student;
import org.hackathon.data.po.User;
import org.hackathon.data.vo.AuthorityUserVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorityService {

    private final AuthorityMapper authorityMapper;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final StudentMapper studentMapper;

    private void verifyUser(Integer userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
    }

    private boolean eventIsLive(Integer eventId) {
        return !eventMapper.selectById(eventId).getLiveEnd().isBefore(LocalDateTime.now());
    }

    public void createSuper(Integer userId) {
        verifyUser(userId);
        try {
            Authority authority = new Authority(null, userId, AuthorityEnum.SUPER, null,
                    LocalDateTime.now());
            authorityMapper.insert(authority);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.AUTHORITY_REPEAT);
        }
    }

    @Transactional
    public void deleteSuper(Integer userId) {
        long count = authorityMapper.selectCount(
                new LambdaQueryWrapper<Authority>().eq(Authority::getType, AuthorityEnum.SUPER)
                        .last("FOR UPDATE")
        );
        if (count == 1) {
            throw new BusinessException(ResultCode.LAST_SUPER);
        }
        authorityMapper.delete(
                new LambdaQueryWrapper<Authority>().eq(Authority::getType, AuthorityEnum.SUPER)
                        .eq(Authority::getUserId, userId)
        );
    }

    public void createAdmin(Integer userId, Integer eventId) {
        verifyUser(userId);
        try {
            Authority authority = new Authority(null, userId, AuthorityEnum.ADMIN, eventId,
                    LocalDateTime.now());
            authorityMapper.insert(authority);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.AUTHORITY_REPEAT);
        }
    }

    public void deleteAdmin(Integer userId, Integer eventId) {
        long count = authorityMapper.selectCount(
                new LambdaQueryWrapper<Authority>().eq(Authority::getType, AuthorityEnum.ADMIN)
                        .eq(Authority::getEventId, eventId)
        );
        if (count == 1 && eventIsLive(eventId)) {
            throw new BusinessException(ResultCode.LAST_ADMIN);
        }
        authorityMapper.delete(
                new LambdaQueryWrapper<Authority>().eq(Authority::getType, AuthorityEnum.ADMIN)
                .eq(Authority::getUserId, userId).eq(Authority::getEventId, eventId)
        );
    }

    public void createJudge(Integer userId, Integer eventId) {
        verifyUser(userId);
        try {
            Authority authority = new Authority(null, userId, AuthorityEnum.JUDGE, eventId,
                    LocalDateTime.now());
            authorityMapper.insert(authority);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.AUTHORITY_REPEAT);
        }
    }

    public void deleteJudge(Integer userId, Integer eventId) {
        authorityMapper.delete(
                new LambdaQueryWrapper<Authority>().eq(Authority::getType, AuthorityEnum.JUDGE)
                        .eq(Authority::getUserId, userId).eq(Authority::getEventId, eventId)
        );
    }

    public List<AuthorityUserVO> getAuthorityListByEvent(Integer eventId) {
        List<Authority> authorities = authorityMapper.selectList(
                new LambdaQueryWrapper<Authority>().eq(Authority::getEventId, eventId)
                        .select(Authority::getUserId, Authority::getType)
        ).stream().sorted(Comparator.comparing(Authority::getTypeValue)).toList();
        if (authorities.isEmpty()) return List.of();
        List<Integer> ids = authorities.stream().map(Authority::getUserId).distinct().toList();
        List<User> users = userMapper.selectByIds(ids);
        Map<Integer, Boolean> studentFlags = users.stream()
                .collect(Collectors.toMap(User::getUserId, User::getStudentFlag));
        Map<Integer, String> exUserNames = users.stream()
                .collect(Collectors.toMap(User::getUserId, User::getUsername));
        Map<Integer, String> studentNames = studentMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Student::getUserId, Student::getName));
        return authorities.stream().map(a -> new AuthorityUserVO(a.getUserId(),
                (studentFlags.get(a.getUserId()) ? studentNames.get(a.getUserId())
                        : exUserNames.get(a.getUserId())), a.getType())).toList();
    }
}
