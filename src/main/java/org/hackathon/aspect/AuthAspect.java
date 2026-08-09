package org.hackathon.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final LocalJwtUtils LocalJwtUtils;

    @Pointcut("@annotation(org.hackathon.annotation.Auth)")
    public void authPointcut() {
    }

    @Around("authPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        if (token == null) {
            throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token.isBlank()) {
            throw new BusinessException(ResultCode.TOKEN_IS_BLANK);
        }
        LocalJwt jwt = LocalJwtUtils.parseToken(token);
        if (jwt.getId() == -1) {
            throw new BusinessException(ResultCode.NOT_REGISTERED);
        }
        request.setAttribute("jwt", jwt);
        return joinPoint.proceed();
    }
}