package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.enums.PhaseStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.BriefPhaseVO;
import org.hackathon.data.vo.GetActivePhaseVO;
import org.hackathon.data.vo.GetTrackVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackService {
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;
    private final EventService eventService;

    public GetTrackVO getTrack(Integer trackId) {
        Track track = trackMapper.selectById(trackId);
        List<BriefPhaseVO> list = phaseMapper.selectList(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId)
                        .select(Phase::getPhaseId,Phase::getName)
        ).stream().map(po -> new BriefPhaseVO(po.getPhaseId(), po.getName())).toList();
        return new GetTrackVO(
                track.getTrackId(), track.getName(), track.getDescMd(), track.getVersion(), list
        );
    }

    public boolean updateTrack(UpdateTrackDTO dto, Integer trackId) {
        Track track = trackMapper.selectById(trackId);
        if (!track.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        Event event = eventService.updateStatus(eventMapper.selectById(track.getEventId()));
        if (event.getLiveEnd().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_OFF);
        }
        boolean update = false;
        if (dto.getName() != null && !dto.getName().equals(track.getName())) {
            track.setName(dto.getName());
            update = true;
        }
        if (dto.getDescMd() != null && !dto.getDescMd().equals(track.getDescMd())) {
            track.setDescMd(dto.getDescMd());
            update = true;
        }
        if (update) {
            track.setUpdateTime(LocalDateTime.now());
            if (trackMapper.updateById(track) == 0) {
                throw new BusinessException(ResultCode.RESOURCE_UPDATED);
            }
        }
        return update;
    }

    public GetActivePhaseVO getActivePhase(Integer trackId) {
        LocalDateTime now = LocalDateTime.now();
        Phase phase = phaseMapper.selectOne(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId).le(Phase::getSubmitBeg, now)
                        .ge(Phase::getReviewEnd, now)
        );
        GetActivePhaseVO vo = new GetActivePhaseVO();
        if (phase == null) {
            vo.setExisted(false);
            return vo;
        }
        vo.setExisted(true);
        BeanUtils.copyProperties(phase, vo);
        Duration duration;
        if (now.isBefore(vo.getSubmitEnd())) {
            vo.setStatus(PhaseStatus.SUBMIT);
            duration = Duration.between(now, vo.getSubmitEnd());
        } else if (now.isBefore(vo.getReviewBeg())) {
            vo.setStatus(PhaseStatus.MID);
            duration = Duration.between(now, vo.getReviewBeg());
        } else {
            vo.setStatus(PhaseStatus.REVIEW);
            duration = Duration.between(now, vo.getReviewEnd());
        }
        vo.setLeftDays(duration.toDaysPart());
        vo.setLeftHours(duration.toHoursPart());
        vo.setLeftMinutes(duration.toMinutesPart());
        return vo;
    }
}
