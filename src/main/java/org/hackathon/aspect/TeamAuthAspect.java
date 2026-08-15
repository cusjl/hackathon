package org.hackathon.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hackathon.annotation.TeamAuth;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Team;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.*;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class TeamAuthAspect {

    private final LocalJwtUtils localJwtUtils;
    private final TeamMapper teamMapper;

    @Pointcut("@annotation(org.hackathon.annotation.TeamAuth)")
    public void teamAuthPointcut() {
    }

    @Around("teamAuthPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        TeamAuth auth = signature.getMethod().getAnnotation(TeamAuth.class);
        LocalJwt jwt = localJwtUtils.extractJwt(request);
        if (!jwt.getStudentFlag()) {
            throw new BusinessException(ResultCode.NOT_STUDENT);
        }
        Integer teamId = extractTeamId(request);
        if (teamId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未解析到teamId");
        }
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ResultCode.TEAM_NOT_FOUND);
        }
        if (auth.onlyLeader() && !team.getLeaderId().equals(jwt.getUserId())) {
            throw new BusinessException(ResultCode.NOT_LEADER);
        }
        request.setAttribute("team", team);
        request.setAttribute("jwt", jwt);
        return joinPoint.proceed();
    }

    private Integer extractTeamId(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars != null && uriVars.containsKey("teamId")) {
            try {
                return Integer.valueOf(uriVars.get("teamId"));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}