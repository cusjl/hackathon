package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.UpdateRegistrationDTO;
import org.hackathon.data.enums.EventStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Registration;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.RegistrationVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.RegistrationMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationMapper regMapper;
    private final TrackMapper trackMapper;

    public void createRegistration(EventContext context, Integer userId) {
        Event event = context.getEvent();
        if (event.getStatus() != EventStatus.REG) {
            throw new BusinessException(ResultCode.NOT_REGISTER_TIME);
        }
        long count  = regMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .in(Registration::getEventId, event.getEventId())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.REGISTRATION_REPEAT);
        }
        Registration registration = new Registration(
                null, userId, event.getEventId(), context.getTrack().getTrackId(),
                null, 1, LocalDateTime.now(), LocalDateTime.now()
        );
        regMapper.insert(registration);
    }

    public RegistrationVO getRegistration(EventContext context, Integer userId) {
        Event event = context.getEvent();
        if (event.getStatus() == EventStatus.PREP) {
            return new RegistrationVO(false, null, null, null);
        }
        Registration po = regMapper.selectOne(
                new LambdaQueryWrapper<Registration>().eq(Registration::getUserId, userId)
                        .eq(Registration::getEventId, event.getEventId())
        );
        if (po == null) {
            return new RegistrationVO(false, null, null, null);
        }
        return new RegistrationVO(true, po.getTrackId(), po.getTeamId(), po.getVersion());
    }

    private Registration verifyCondition(Integer userId, Event event) {
        if (event.getStatus() != EventStatus.REG) {
            throw new BusinessException(ResultCode.NOT_REGISTER_TIME);
        }
        Registration registration = regMapper.selectOne(
                new LambdaQueryWrapper<Registration>().eq(Registration::getUserId, userId)
                        .eq(Registration::getEventId, event.getEventId())
        );
        if (registration == null) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        if (registration.getTeamId() != null) {
            throw new BusinessException(ResultCode.ALREADY_TEAMED);
        }
        return registration;
    }

    public void updateRegistration(UpdateRegistrationDTO dto, EventContext context, Integer userId) {
        Event event = context.getEvent();
        Registration registration = verifyCondition(userId, event);
        Track track = trackMapper.selectById(dto.getTrackId());
        if (track == null || !track.getEventId().equals(event.getEventId())) {
            throw new BusinessException(ResultCode.TRACK_NOT_FOUND, "当前赛事下没有对应赛道");
        }
        registration.setTrackId(dto.getTrackId());
        registration.setVersion(dto.getVersion());
        registration.setUpdateTime(LocalDateTime.now());
        if (regMapper.updateById(registration) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    public void deleteRegistration(EventContext context, Integer userId) {
        Event event = context.getEvent();
        Registration registration = verifyCondition(userId, event);
        regMapper.deleteById(registration.getId());
    }
}
