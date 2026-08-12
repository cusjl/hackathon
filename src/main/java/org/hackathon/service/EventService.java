package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreateEventDTO;
import org.hackathon.data.dto.CreateTrackDTO;
import org.hackathon.data.dto.UpdateEventDTO;
import org.hackathon.data.enums.EventStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.GetEventVO;
import org.hackathon.data.vo.BriefTrackVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;

    private void verifyEventTime(
            LocalDateTime regBeg, LocalDateTime regEnd, LocalDateTime liveBeg, LocalDateTime liveEnd) {
        if (regBeg.isBefore(LocalDateTime.now()) || regEnd.isBefore(regBeg) ||
                liveBeg.isBefore(regEnd) || liveEnd.isBefore(liveBeg)) {
            throw new BusinessException(ResultCode.EVENT_TIME_CONFLICT);
        }
    }

    //每次用eventMapper查表调用，维护表属性
    public Event updateStatus(Event event){
        boolean flag = false;
        int value = event.getStatus().getValue();
        LocalDateTime now = LocalDateTime.now();
        while (!flag){
            switch (event.getStatus()){
                case PREP:
                    if (now.isBefore(event.getRegBeg())) flag = true;
                    else event.setStatus(EventStatus.REG);
                    break;
                case REG:
                    if (now.isBefore(event.getRegEnd())) flag = true;
                    else event.setStatus(EventStatus.MID);
                    break;
                case MID:
                    if (now.isBefore(event.getLiveBeg())) flag = true;
                    else event.setStatus(EventStatus.LIVE);
                    break;
                case LIVE:
                    if (now.isBefore(event.getLiveEnd())) flag = true;
                    else event.setStatus(EventStatus.END);
                    break;
                default:
                    flag = true;
            }
        }
        if (value < event.getStatus().getValue()) {
            LambdaUpdateWrapper<Event> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Event::getEventId, event.getEventId()).set(Event::getStatus, event.getStatus());
            eventMapper.update(wrapper);
        }
        return event;
    }

    public Integer createEvent(CreateEventDTO dto) {
        verifyEventTime(dto.getRegBeg(), dto.getRegEnd(), dto.getLiveBeg(), dto.getLiveEnd());
        Event event = new Event(null, dto.getName(), EventStatus.PREP, dto.getRegBeg(), dto.getRegEnd(),
        dto.getLiveBeg(), dto.getLiveEnd(), dto.getIntroduction(), dto.getTags(), "", 1,
                LocalDateTime.now(), LocalDateTime.now());
        eventMapper.insert(event);
        return event.getEventId();
    }

    public Integer createTrack(CreateTrackDTO dto, Integer eventId) {
        Event event = updateStatus(eventMapper.selectById(eventId));
        if (event.getRegBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        LambdaUpdateWrapper<Track> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Track::getEventId, eventId).eq(Track::getName, dto.getName());
        if (trackMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.TRACK_ALREADY_EXIST);
        }
        Track track = new Track(null, eventId, dto.getName(), dto.getDescMd(), 1,
                LocalDateTime.now(), LocalDateTime.now());
        trackMapper.insert(track);
        return track.getTrackId();
    }

    public GetEventVO getEvent(Integer eventId) {
        Event event = updateStatus(eventMapper.selectById(eventId));
        GetEventVO vo = new GetEventVO();
        BeanUtils.copyProperties(event, vo);
        List<BriefTrackVO> list = trackMapper.selectList(
                new LambdaQueryWrapper<Track>().eq(Track::getEventId, eventId)
                        .select(Track::getTrackId, Track::getName)
        ).stream().map(po -> new BriefTrackVO(po.getTrackId(), po.getName())).toList();
        vo.setTracks(list);
        return vo;
    }

    private List<Integer> getTrackIds(Integer eventId) {
        LambdaQueryWrapper<Track> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Track::getEventId, eventId).select(Track::getTrackId);
        return trackMapper.selectList(wrapper).stream().map(Track::getTrackId).toList();
    }

    public boolean updateEvent(UpdateEventDTO dto, Integer eventId) {
        Event event = updateStatus(eventMapper.selectById(eventId));
        if (event.getRegBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        if (!event.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        boolean update = false;
        if (dto.getRegBeg() != null && !dto.getRegBeg().equals(event.getRegBeg())) {
            event.setRegBeg(dto.getRegBeg());
            update = true;
        }
        if (dto.getRegEnd() != null && !dto.getRegEnd().equals(event.getRegEnd())) {
            event.setRegEnd(dto.getRegEnd());
            update = true;
        }
        if (dto.getLiveBeg() != null && !dto.getLiveBeg().equals(event.getLiveBeg())) {
            event.setLiveBeg(dto.getLiveBeg());
            update = true;
        }
        if (dto.getLiveEnd() != null && !dto.getLiveEnd().equals(event.getLiveEnd())) {
            event.setLiveEnd(dto.getLiveEnd());
            update = true;
        }
        verifyEventTime(event.getRegBeg(), event.getRegEnd(), event.getLiveBeg(), event.getLiveEnd());
        List<Integer> trackIds = getTrackIds(eventId);
        LambdaUpdateWrapper<Phase> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Phase::getTrackId, trackIds).and(w ->
                w.lt(Phase::getSubmitBeg, event.getLiveBeg()).or().gt(Phase::getReviewEnd, event.getLiveEnd()));
        if (phaseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PHASE_EVENT_TIME_CONFLICT);
        }
        if (dto.getName() != null && !dto.getName().equals(event.getName())) {
            event.setName(dto.getName());
            update = true;
        }
        if (dto.getIntroduction() != null && !dto.getIntroduction().equals(event.getIntroduction())) {
            event.setIntroduction(dto.getIntroduction());
            update = true;
        }
        if (dto.getTags() != null && !dto.getTags().equals(event.getTags())) {
            event.setTags(dto.getTags());
            update = true;
        }
        if (dto.getNotice() != null && !dto.getNotice().equals(event.getNotice())) {
            event.setNotice(dto.getNotice());
        }
        if (update) {
            event.setUpdateTime(LocalDateTime.now());
            //乐观锁检查
            if (eventMapper.updateById(event) == 0) {
                throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            }
        }
        return update;
    }
}
