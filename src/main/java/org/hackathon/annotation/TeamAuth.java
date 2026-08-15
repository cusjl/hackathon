package org.hackathon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    先提取token验证学生身份
    再提取路径变量teamId做存在性验证
    onlyLeader额外判断是否为队长
    验证后将jwt和team存入Attribute
    评委/赛管请改用@EventAuth
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TeamAuth {
    boolean onlyLeader() default false;
}