package org.hackathon.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.hackathon.annotation.Auth;
import org.hackathon.data.enums.AuthorityEnum;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Authority;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.AuthorityMapper;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final LocalJwtUtils localJwtUtils;
    private final AuthorityMapper authorityMapper;

    @Pointcut("@annotation(org.hackathon.annotation.Auth)")
    public void authPointcut() {
    }

    @Around("authPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        LocalJwt jwt = localJwtUtils.extractJwt(request);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Auth auth = signature.getMethod().getAnnotation(Auth.class);
        if (auth.onlyStudent() && !jwt.getStudentFlag()) {
            throw new BusinessException(ResultCode.NOT_STUDENT);
        }
        if (auth.onlySuper()) {
            long count = authorityMapper.selectCount(
                    new LambdaQueryWrapper<Authority>().eq(Authority::getUserId, jwt.getUserId())
                            .eq(Authority::getType, AuthorityEnum.SUPER)
            );
            if (count == 0) {
                throw new BusinessException(ResultCode.NOT_SUPER, "需要超管权限");
            }
        }
        request.setAttribute("jwt", jwt);
        return joinPoint.proceed();
    }
}