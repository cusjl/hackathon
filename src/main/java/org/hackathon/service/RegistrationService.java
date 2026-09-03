package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.hackathon.security.Context;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.UpdateRegistrationDTO;
import org.hackathon.data.enums.EventStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Registration;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.EventBriefVO;
import org.hackathon.data.vo.RegistrationVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.RegistrationMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationMapper regMapper;
    private final TrackMapper trackMapper;
    private final EventMapper eventMapper;

    public void createRegistration(Context ctx) {
        Integer userId = ctx.userId();
        Event event = ctx.event();
        long count  = regMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .in(Registration::getEventId, event.getEventId())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.REGISTRATION_REPEAT);
        }
        Registration registration = new Registration(
                null, userId, event.getEventId(), ctx.track().getTrackId(),
                null, 1, LocalDateTime.now(), LocalDateTime.now()
        );
        regMapper.insert(registration);
    }

    public RegistrationVO getRegistration(Context ctx) {
        Integer userId = ctx.userId();
        Event event = ctx.event();
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

    public IPage<EventBriefVO> getRegisteredEventPage(PageParamDTO param, Context ctx) {
        Page<Registration> registrationPage = regMapper.selectPage(
                new Page<>(param.getPage(), param.getSize()),
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, ctx.userId())
                        .orderByDesc(Registration::getUpdateTime)
                        .select(Registration::getEventId)
        );
        if (registrationPage.getRecords().isEmpty()) {
            return new Page<>(param.getPage(), param.getSize(), registrationPage.getTotal());
        }

        List<Integer> eventIds = registrationPage.getRecords().stream()
                .map(Registration::getEventId).toList();
        Map<Integer, Event> eventsById = eventMapper.selectList(
                new LambdaQueryWrapper<Event>().in(Event::getEventId, eventIds)
        ).stream().collect(Collectors.toMap(Event::getEventId, Function.identity()));
        List<EventBriefVO> records = registrationPage.getRecords().stream()
                .map(Registration::getEventId)
                .map(eventsById::get)
                .map(event -> new EventBriefVO(event.getEventId(), event.getName(), event.getStatus(),
                        event.getRegBeg(), event.getRegEnd(), event.getLiveBeg(), event.getLiveEnd(), event.getTags()))
                .toList();

        return new Page<EventBriefVO>(param.getPage(), param.getSize(), registrationPage.getTotal())
                .setRecords(records);
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

    public void updateRegistration(UpdateRegistrationDTO dto, Context ctx) {
        Integer userId = ctx.userId();
        Event event = ctx.event();
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

    public void deleteRegistration(Context ctx) {
        Integer userId = ctx.userId();
        Event event = ctx.event();
        Registration registration = verifyCondition(userId, event);
        regMapper.deleteById(registration.getId());
    }
}
