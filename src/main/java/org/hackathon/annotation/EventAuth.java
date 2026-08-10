package org.hackathon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    支持提取路径变量中的eventId，实现相关校验
    首先支持eventId合法性的校验
    权限校验有三个模式：
    GUEST   不进行权限校验
    ADMIN   对应赛事的赛管或超管
    JUDGE   对应赛事的评委
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventAuth {
    String value();
}