package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.*;
import org.hackathon.data.enums.EventStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.EventBriefVO;
import org.hackathon.data.vo.EventInfoVO;
import org.hackathon.data.vo.TrackBriefVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;
    private final AuthorityMapper authorityMapper;

    private void verifyEventTime(
            LocalDateTime regBeg, LocalDateTime regEnd, LocalDateTime liveBeg, LocalDateTime liveEnd) {
        if (regBeg.isBefore(LocalDateTime.now()) || regEnd.isBefore(regBeg) ||
                liveBeg.isBefore(regEnd) || liveEnd.isBefore(liveBeg)) {
            throw new BusinessException(ResultCode.INVALID_EVENT_TIME);
        }
    }

    public Integer createEvent(CreateEventDTO dto) {
        verifyEventTime(dto.getRegBeg(), dto.getRegEnd(), dto.getLiveBeg(), dto.getLiveEnd());
        Event event = new Event(null, dto.getName(), dto.getRegBeg(), dto.getRegEnd(),
        dto.getLiveBeg(), dto.getLiveEnd(), dto.getIntroduction(), dto.getTags(), "", 1,
                LocalDateTime.now(), LocalDateTime.now());
        try {
            eventMapper.insert(event);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_EXIST);
        }
        return event.getEventId();
    }

    public Integer createTrack(CreateTrackDTO dto, EventContext context) {
        Event event = context.getEvent();
        if (event.getRegBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        Track track = new Track(null, event.getEventId(), dto.getName(), dto.getDescMd(),
                1, LocalDateTime.now(), LocalDateTime.now());
        try {
            trackMapper.insert(track);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.TRACK_ALREADY_EXIST);
        }
        return track.getTrackId();
    }

    public EventInfoVO getEvent(EventContext context) {
        Event event = context.getEvent();
        EventInfoVO vo = new EventInfoVO();
        BeanUtils.copyProperties(event, vo);
        List<TrackBriefVO> list = trackMapper.selectList(
                new LambdaQueryWrapper<Track>().eq(Track::getEventId, event.getEventId())
                        .select(Track::getTrackId, Track::getName)
        ).stream().map(po -> new TrackBriefVO(po.getTrackId(), po.getName())).toList();
        vo.setTracks(list);
        return vo;
    }

    private List<Integer> getTrackIds(Integer eventId) {
        LambdaQueryWrapper<Track> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Track::getEventId, eventId).select(Track::getTrackId);
        return trackMapper.selectList(wrapper).stream().map(Track::getTrackId).toList();
    }

    public void updateEvent(UpdateEventDTO dto, EventContext context) {
        verifyEventTime(dto.getRegBeg(), dto.getRegEnd(), dto.getLiveBeg(), dto.getLiveEnd());
        Event event = context.getEvent();
        if (event.getRegBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        if (!event.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        event.setRegBeg(dto.getRegBeg());
        event.setRegEnd(dto.getRegEnd());
        event.setLiveBeg(dto.getLiveBeg());
        event.setLiveEnd(dto.getLiveEnd());
        List<Integer> trackIds = getTrackIds(event.getEventId());
        LambdaUpdateWrapper<Phase> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Phase::getTrackId, trackIds).and(w ->
                w.lt(Phase::getSubmitBeg, event.getLiveBeg()).or().gt(Phase::getReviewEnd, event.getLiveEnd()));
        if (phaseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PHASE_EVENT_TIME_CONFLICT);
        }
        event.setName(dto.getName());
        event.setIntroduction(dto.getIntroduction());
        event.setTags(dto.getTags());
        event.setNotice(dto.getNotice());
        event.setUpdateTime(LocalDateTime.now());
        //乐观锁检查
        if (eventMapper.updateById(event) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    public void deleteEvent(EventContext context) {
        if (!(context.getEvent().getStatus() == EventStatus.PREP)) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
            Integer eventId = context.getEvent().getEventId();
        long count = trackMapper.selectCount(new LambdaQueryWrapper<Track>().eq(Track::getEventId, eventId));
        if (count > 0) {
            throw new BusinessException(ResultCode.BINDING_TRACK);
        }
        authorityMapper.delete(
                new LambdaQueryWrapper<Authority>().eq(Authority::getEventId, eventId)
        );
        eventMapper.deleteById(eventId);
    }

    private void applyStatuses(LambdaQueryWrapper<Event> w, List<EventStatus> statuses) {
        boolean flag = false;
        LocalDateTime now = LocalDateTime.now();
        if (statuses.contains(EventStatus.PREP)) {
            w.ge(Event::getRegBeg, now);
            flag = true;
        }
        if (statuses.contains(EventStatus.REG)) {
            if (flag) w.or();
            w.ge(Event::getRegEnd, now).le(Event::getRegBeg, now);
            flag = true;
        }
        if (statuses.contains(EventStatus.MID)) {
            if (flag) w.or();
            w.ge(Event::getLiveBeg, now).le(Event::getRegEnd, now);
            flag = true;
        }
        if (statuses.contains(EventStatus.LIVE)) {
            if (flag) w.or();
            w.ge(Event::getLiveEnd, now).le(Event::getLiveBeg, now);
            flag = true;
        }
        if (statuses.contains(EventStatus.END)) {
            if (flag) w.or();
            w.le(Event::getLiveEnd, now);
        }
    }

    public IPage<EventBriefVO> getEventPage(QueryEventDTO dto, PageParamDTO param) {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<Event>()
                .like(StringUtils.hasText(dto.getName()), Event::getName, dto.getName());
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            for (String tag : dto.getTags().stream().distinct().toList()) {
                wrapper.like(StringUtils.hasText(tag), Event::getTags, tag);
            }
        }
        if (dto.getStatuses() != null && !dto.getStatuses().isEmpty()) {
            List<EventStatus> statuses = dto.getStatuses().stream().distinct().toList();
            wrapper.and(w ->applyStatuses(w, statuses));
        }
        wrapper.orderByDesc(Event::getUpdateTime)
        .select(Event::getEventId, Event::getName, Event::getRegBeg, Event::getRegEnd,
                Event::getLiveBeg, Event::getLiveEnd, Event::getTags);
        return eventMapper.selectPage(new Page<>(param.getPage(), param.getSize()), wrapper)
                .convert(po -> new EventBriefVO(po.getEventId(), po.getName(), po.getStatus(),
                        po.getRegBeg(), po.getRegEnd(), po.getLiveBeg(), po.getLiveEnd(), po.getTags()));
    }
}
