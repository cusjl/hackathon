package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.security.Context;
import org.hackathon.data.dto.CreatePhaseDTO;
import org.hackathon.data.dto.UpdateTrackDTO;
import org.hackathon.data.enums.EventStatus;
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
import org.springframework.transaction.annotation.Transactional;

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

    public TrackInfoVO getTrack(Context ctx) {
        Track track = ctx.track();
        List<PhaseBriefVO> list = phaseMapper.selectList(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, track.getTrackId())
                        .select(Phase::getPhaseId, Phase::getName, Phase::getSubmitBeg, Phase::getSubmitEnd,
                                Phase::getReviewBeg, Phase::getReviewEnd)
        ).stream().map(po -> new PhaseBriefVO(po.getPhaseId(), po.getName(), po.getStatus(),
                po.getSubmitBeg(), po.getSubmitEnd(), po.getReviewBeg(), po.getReviewEnd())).toList();
        return new TrackInfoVO(
                track.getTrackId(), track.getName(), track.getDescMd(), list, track.getVersion(),
                track.getEventId(), ctx.event().getName()
        );
    }

    public void updateTrack(UpdateTrackDTO dto, Context ctx) {
        Track track = ctx.track();
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

    public PhaseIdVO createPhase(CreatePhaseDTO dto, Context ctx) {
        verifyPhaseTime(dto.getSubmitBeg(), dto.getSubmitEnd(), dto.getReviewBeg(), dto.getReviewEnd());
        Integer trackId = ctx.track().getTrackId();
        Event event = ctx.event();
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
            if (phase.getName().equals(dto.getName())) {
                throw new BusinessException(ResultCode.PHASE_ALREADY_EXIST);
            }
            if (!(phase.getReviewEnd().isBefore(dto.getSubmitBeg()) ||
                    dto.getReviewEnd().isBefore(phase.getSubmitBeg()))) {
                throw new BusinessException(ResultCode.PHASE_TIME_CONFLICT);
            }
        }
        Phase phase = new Phase(null, trackId, dto.getName(), dto.getSubmitBeg(), dto.getSubmitEnd(),
                dto.getReviewBeg(), dto.getReviewEnd(), dto.getBlindReview(), dto.getMidCheck(), dto.getManualPick(),
                dto.getPassRate() == null ? BigDecimal.ONE : dto.getPassRate(), dto.getPoll(),
                null, null, null, null,
                new SubmissionConfig(), 1, LocalDateTime.now(), LocalDateTime.now()
        );
        phaseMapper.insert(phase);
        return new PhaseIdVO(phase.getPhaseId());
    }

    @Transactional
    public void deleteTrack(Context ctx) {
        if (!(ctx.event().getStatus() == EventStatus.PREP)) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_REG);
        }
        Integer trackId = ctx.track().getTrackId();
        LambdaQueryWrapper<Phase> wrapper = new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, trackId);
        if (phaseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.BINDING_PHASE);
        }
        trackMapper.deleteById(trackId);
    }
}
