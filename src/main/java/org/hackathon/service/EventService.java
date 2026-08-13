package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.CreateEventDTO;
import org.hackathon.data.dto.CreateTrackDTO;
import org.hackathon.data.dto.UpdateEventDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.EventInfoVO;
import org.hackathon.data.vo.TrackBriefVO;
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
            throw new BusinessException(ResultCode.INVALID_EVENT_TIME);
        }
    }

    public Integer createEvent(CreateEventDTO dto) {
        verifyEventTime(dto.getRegBeg(), dto.getRegEnd(), dto.getLiveBeg(), dto.getLiveEnd());
        Event event = new Event(null, dto.getName(), dto.getRegBeg(), dto.getRegEnd(),
        dto.getLiveBeg(), dto.getLiveEnd(), dto.getIntroduction(), dto.getTags(), "", 1,
                LocalDateTime.now(), LocalDateTime.now());
        eventMapper.insert(event);
        return event.getEventId();
    }

    public Integer createTrack(CreateTrackDTO dto, EventContext context) {
        Event event = context.getEvent();
        if (event.getRegBeg().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        LambdaUpdateWrapper<Track> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Track::getEventId, event.getEventId()).eq(Track::getName, dto.getName());
        if (trackMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.TRACK_ALREADY_EXIST);
        }
        Track track = new Track(null, event.getEventId(), dto.getName(), dto.getDescMd(),
                1, LocalDateTime.now(), LocalDateTime.now());
        trackMapper.insert(track);
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
}
