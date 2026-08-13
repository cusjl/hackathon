package org.hackathon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    普通校验
    校验后将token解码得到的jwt存至RequestAttribute
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
    boolean onlyStudent() default false;
    boolean onlySuper() default false;
    boolean onlyExtern() default false;
}