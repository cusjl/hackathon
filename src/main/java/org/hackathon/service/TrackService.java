package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.CreatePhaseDTO;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.enums.PhaseStatus;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.SubmissionConfig;
import org.hackathon.data.po.Track;
import org.hackathon.data.vo.PhaseBriefVO;
import org.hackathon.data.vo.PhaseIdVO;
import org.hackathon.data.vo.PhaseActiveVO;
import org.hackathon.data.vo.TrackInfoVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackService {
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;

    public TrackInfoVO getTrack(Integer trackId) {
        Track track = trackMapper.selectById(trackId);
        List<PhaseBriefVO> list = phaseMapper.selectList(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId)
                        .select(Phase::getPhaseId, Phase::getName, Phase::getSubmitBeg, Phase::getSubmitEnd,
                                Phase::getReviewBeg, Phase::getReviewEnd)
        ).stream().map(po -> new PhaseBriefVO(po.getPhaseId(), po.getName(), po.getStatus(),
                po.getSubmitBeg(), po.getSubmitEnd(), po.getReviewBeg(), po.getReviewEnd())).toList();
        return new TrackInfoVO(
                track.getTrackId(), track.getName(), track.getDescMd(), track.getVersion(), list
        );
    }

    public void updateTrack(UpdateTrackDTO dto, Integer trackId) {
        Track track = trackMapper.selectById(trackId);
        if (!track.getVersion().equals(dto.getVersion())) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        Event event = eventMapper.selectById(track.getEventId());
        if (event.getLiveEnd().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_END);
        }
        track.setName(dto.getName());
        track.setDescMd(dto.getDescMd());
        track.setUpdateTime(LocalDateTime.now());
        if (trackMapper.updateById(track) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    public PhaseActiveVO getActivePhase(Integer trackId) {
        LocalDateTime now = LocalDateTime.now();
        Phase phase = phaseMapper.selectOne(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId).le(Phase::getSubmitBeg, now)
                        .ge(Phase::getReviewEnd, now)
        );
        PhaseActiveVO vo = new PhaseActiveVO();
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

    private void verifyPhaseTime(LocalDateTime submitBeg, LocalDateTime submitEnd,
                                 LocalDateTime reviewBeg, LocalDateTime reviewEnd) {
        if (submitBeg.isBefore(LocalDateTime.now()) || submitEnd.isBefore(submitBeg) ||
                reviewBeg.isBefore(submitEnd) || reviewEnd.isBefore(reviewBeg)) {
            throw new BusinessException(ResultCode.INVALID_PHASE_TIME);
        }
    }

    public PhaseIdVO createPhase(CreatePhaseDTO dto, Integer trackId) {
        verifyPhaseTime(dto.getSubmitBeg(), dto.getSubmitEnd(), dto.getReviewBeg(), dto.getReviewEnd());
        Track track = trackMapper.selectById(trackId);
        Event event = eventMapper.selectById(track.getEventId());
        if (event.getLiveEnd().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_END);
        } else if (event.getLiveEnd().isBefore(dto.getReviewEnd()) ||
                dto.getSubmitBeg().isBefore(event.getLiveBeg())) {
            throw new BusinessException(ResultCode.PHASE_EVENT_TIME_CONFLICT);
        }
        List<Phase> phases = phaseMapper.selectList(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId)
        );
        for (Phase phase : phases) {
            if (!(phase.getReviewEnd().isBefore(dto.getSubmitBeg()) ||
                    dto.getReviewEnd().isBefore(phase.getSubmitBeg()))) {
                throw new BusinessException(ResultCode.PHASE_TIME_CONFLICT);
            }
            if (phase.getName().equals(dto.getName())) {
                throw new BusinessException(ResultCode.PHASE_ALREADY_EXIST);
            }
        }
        Phase phase = new Phase(null, trackId, dto.getName(), dto.getSubmitBeg(), dto.getSubmitEnd(),
                dto.getReviewBeg(), dto.getReviewEnd(), dto.getBlindReview(), dto.getMidCheck(), dto.getManualPick(),
                dto.getPassRate() == null ? BigDecimal.ONE : dto.getPassRate(), dto.getPoll(),
                new SubmissionConfig(), 1, LocalDateTime.now(), LocalDateTime.now()
        );
        phaseMapper.insert(phase);
        return new PhaseIdVO(phase.getPhaseId());
    }
}
