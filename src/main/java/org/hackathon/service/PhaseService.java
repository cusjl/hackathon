package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.dto.UpdatePhaseConfigDTO;
import org.hackathon.data.dto.UpdatePhaseDTO;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.SubmissionConfig;
import org.hackathon.data.vo.PhaseInfoVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.PhaseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhaseService {

    private final PhaseMapper phaseMapper;

    private void verifyPhaseTime(LocalDateTime submitBeg, LocalDateTime submitEnd,
                                 LocalDateTime reviewBeg, LocalDateTime reviewEnd) {
        if (submitBeg.isBefore(LocalDateTime.now()) || submitEnd.isBefore(submitBeg) ||
                reviewBeg.isBefore(submitEnd) || reviewEnd.isBefore(reviewBeg)) {
            throw new BusinessException(ResultCode.INVALID_PHASE_TIME);
        }
    }

    public PhaseInfoVO getPhase(EventContext context) {
        PhaseInfoVO vo = new PhaseInfoVO();
        BeanUtils.copyProperties(context.getPhase(), vo);
        vo.setTrackId(context.getTrack().getTrackId());
        vo.setTrackName(context.getTrack().getName());
        vo.setEventId(context.getEvent().getEventId());
        vo.setEventName(context.getEvent().getName());
        return vo;
    }

    public void updatePhase(UpdatePhaseDTO dto, EventContext context) {
        Phase phase = context.getPhase();
        if (phase.getVersion() > dto.getVersion()) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
        verifyPhaseTime(dto.getSubmitBeg(), dto.getSubmitEnd(), dto.getReviewBeg(), dto.getReviewEnd());
        Event event = context.getEvent();
        if (event.getLiveEnd().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.EVENT_ALREADY_END);
        }  else if (event.getLiveEnd().isBefore(dto.getReviewEnd()) ||
                dto.getSubmitBeg().isBefore(event.getLiveBeg())) {
            throw new BusinessException(ResultCode.PHASE_EVENT_TIME_CONFLICT);
        }
        List<Phase> phases = phaseMapper.selectList(
                new LambdaQueryWrapper<Phase>().eq(Phase::getTrackId, context.getTrack().getTrackId())
                        .ne(Phase::getPhaseId, phase.getPhaseId())
        );
        for (Phase item : phases) {
            if (item.getName().equals(dto.getName())) {
                throw new BusinessException(ResultCode.PHASE_ALREADY_EXIST);
            }
            if (!(item.getReviewEnd().isBefore(dto.getSubmitBeg()) ||
                    dto.getReviewEnd().isBefore(item.getSubmitBeg()))) {
                throw new BusinessException(ResultCode.PHASE_TIME_CONFLICT);
            }
        }
        BeanUtils.copyProperties(dto, phase);
        phase.setUpdateTime(LocalDateTime.now());
        if (phaseMapper.updateById(phase) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

    public void updatePhaseConfig (UpdatePhaseConfigDTO dto, EventContext context) {
        Phase phase = context.getPhase();
        SubmissionConfig config = new SubmissionConfig();
        BeanUtils.copyProperties(dto, config);
        if (!config.getOpenSource()) config.setLicenseType(null);
        if (!config.getZip()) config.setMaxSizeMB(null);
        if (!config.getVideo()) config.setMaxMinute(null);
        phase.setSubmissionConfig(config);
        phase.setVersion(dto.getVersion());
        phase.setUpdateTime(LocalDateTime.now());
        if (phaseMapper.updateById(phase) == 0) {
            throw new BusinessException(ResultCode.RESOURCE_UPDATED);
        }
    }

}
