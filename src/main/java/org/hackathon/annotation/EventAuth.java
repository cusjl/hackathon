package org.hackathon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    支持提取路径变量中的id，实现相关校验
    首先支持相关id合法性的校验:
    var: EVENT | TRACK | PHASE
    权限校验有三个模式(mode)：
    GUEST   不进行权限校验
    ADMIN   对应赛事的赛管或超管
    JUDGE   对应赛事的评委
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventAuth {
    String mode();
    String var();
}