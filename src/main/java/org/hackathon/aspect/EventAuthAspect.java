package org.hackathon.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.context.EventContext;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.data.po.Event;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Track;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class EventAuthAspect {

    private final LocalJwtUtils localJwtUtils;
    private final AuthorityMapper authorityMapper;
    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;

    @Pointcut("@annotation(org.hackathon.annotation.EventAuth)")
    public void eventAuthPointcut() {
    }

    @Around("eventAuthPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        EventAuth auth = signature.getMethod().getAnnotation(EventAuth.class);
        String var = auth.var().toLowerCase();
        if (!List.of("event", "track", "phase").contains(var)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "@EventAuth参数错误：var = " + var.toUpperCase());
        }
        String mode = auth.mode().toUpperCase();
        if (!List.of("GUEST", "ADMIN", "JUDGE").contains(mode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "@EventAuth参数错误：mode = " + mode);
        }
        Integer id = extractId(request, var);
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未解析到" + var + "Id");
        }
        EventContext context = verifyExistence(var, id);
        request.setAttribute("context", context);
        if (mode.equals("GUEST")) {
            return joinPoint.proceed();
        }
        LocalJwt jwt = localJwtUtils.extractJwt(request);
        List<Authority> list = authorityMapper.selectList(
                new LambdaQueryWrapper<Authority>().eq(Authority::getUserId, jwt.getUserId())
                        .and(w -> w.eq(Authority::getType, AuthorityEnum.SUPER)
                                .or().eq(Authority::getEventId, context.getEvent().getEventId()))
        );
        boolean isAdmin = false, isJudge = false;
        for (Authority authority : list) {
            if (authority.getType() == AuthorityEnum.JUDGE) {
                isJudge = true;
            } else {
                isAdmin = true;
            }
        }
        if (mode.equals("ADMIN") && !isAdmin) {
            throw new BusinessException(ResultCode.NOT_ADMIN);
        }
        if (mode.equals("JUDGE") && !isJudge) {
            throw new BusinessException(ResultCode.NOT_JUDGE);
        }
        request.setAttribute("jwt", jwt);
        return joinPoint.proceed();
    }

    private Integer extractId(HttpServletRequest request, String var) {
        @SuppressWarnings("unchecked")
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars != null && uriVars.containsKey(var + "Id")) {
            try {
                return Integer.valueOf(uriVars.get(var + "Id"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private EventContext verifyExistence(String var, Integer id) {
        EventContext context = new EventContext();
        Integer trackId = null, eventId = null;
        switch (var) {
            case "phase":
                Phase phase = phaseMapper.selectById(id);
                if (phase == null) throw new BusinessException(ResultCode.PHASE_NOT_FOUND);
                context.setPhase(phase);
                trackId = phase.getTrackId();
            case "track":
                if (var.equals("track")) trackId = id;
                Track track = trackMapper.selectById(trackId);
                if (track == null) throw new BusinessException(ResultCode.TRACK_NOT_FOUND);
                context.setTrack(track);
                eventId = track.getEventId();
            default:
                if (var.equals("event")) eventId = id;
                Event event = eventMapper.selectById(eventId);
                if (event == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
                context.setEvent(event);
            return context;
        }
    }
}