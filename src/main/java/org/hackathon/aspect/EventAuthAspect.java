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
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.mapper.EventMapper;
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

    @Pointcut("@annotation(org.hackathon.annotation.EventAuth)")
    public void eventAuthPointcut() {
    }

    @Around("eventAuthPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        EventAuth eventAuth = signature.getMethod().getAnnotation(EventAuth.class);
        Integer eventId = extractEventId(request);
        if (eventId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未解析到EventId");
        }
        if (eventMapper.selectById(eventId) == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        String role = eventAuth.value().toUpperCase();
        if (role.equals("GUEST")) {
            return joinPoint.proceed();
        }
        LocalJwt jwt = localJwtUtils.extractJwt(request);
        if (!(role.equals("ADMIN") || role.equals("JUDGE"))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "@EventAuth参数错误：" + role);
        }
        List<Authority> list = authorityMapper.selectList(
                new LambdaQueryWrapper<Authority>().eq(Authority::getUserId, jwt.getUserId())
                        .and(w -> w.eq(Authority::getType, AuthorityEnum.SUPER)
                                .or().eq(Authority::getEventId, eventId))
        );
        boolean isAdmin = false, isJudge = false;
        for (Authority authority : list) {
            if (authority.getType() == AuthorityEnum.JUDGE) {
                isJudge = true;
            } else {
                isAdmin = true;
            }
        }
        if (role.equals("ADMIN") && !isAdmin) {
            throw new BusinessException(ResultCode.NOT_ADMIN);
        }
        if (role.equals("JUDGE") && !isJudge) {
            throw new BusinessException(ResultCode.NOT_JUDGE);
        }
        request.setAttribute("jwt", jwt);
        return joinPoint.proceed();
    }

    private Integer extractEventId(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars != null && uriVars.containsKey("eventId")) {
            try {
                return Integer.valueOf(uriVars.get("eventId"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}