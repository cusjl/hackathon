package org.hackathon.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.exception.BusinessException;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor, HandlerMethodArgumentResolver {

    static final String ATTR = "hackathon.ctx";

    private final LocalJwtUtils jwtUtils;
    private final Context.Deps deps;

    @Override
    public boolean preHandle(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull Object handler) {
        if (req.getDispatcherType() != DispatcherType.REQUEST) return true;
        if (!(handler instanceof HandlerMethod hm)) return true;

        Context ctx = Context.of(req, jwtUtils, deps);
        req.setAttribute(ATTR, ctx);

        ctx.requireValidCredential();
        Require require = hm.getMethodAnnotation(Require.class);
        if (require == null) return true;

        ctx.requireAuthenticated();
        ctx.requireConsistentPath();
        if (Arrays.stream(require.value()).noneMatch(r -> r.test(ctx))) {
            throw new BusinessException(denyCode(require.value()));
        }
        ctx.requireWindow(require.window());
        return true;
    }

    private ResultCode denyCode(Role[] roles) {
        if (roles.length != 1) return ResultCode.UNAUTHORIZED;
        return switch (roles[0]) {
            case STUDENT -> ResultCode.NOT_STUDENT;
            case EXTERN -> ResultCode.NOT_EXTERN;
            case SUPER -> ResultCode.NOT_SUPER;
            case EVENT_ADMIN -> ResultCode.NOT_ADMIN;
            case EVENT_JUDGE, ASSIGNED_JUDGE -> ResultCode.NOT_JUDGE;
            case TEAM_LEADER -> ResultCode.NOT_LEADER;
            default -> ResultCode.UNAUTHORIZED;
        };
    }

    @Override
    public boolean supportsParameter(MethodParameter p) {
        return p.getParameterType() == Context.class;
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter p, ModelAndViewContainer m,
                                  NativeWebRequest w, WebDataBinderFactory f) {
        HttpServletRequest req = w.getNativeRequest(HttpServletRequest.class);
        return req == null ? null : req.getAttribute(ATTR);
    }
}
