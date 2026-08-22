package org.hackathon.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Require {

    Role[] value() default {Role.LOGGED_IN};

    Window window() default Window.ANY;

    enum Window {
        ANY,
        REGISTRATION,
        SUBMIT,
        REVIEW,
        VOTE,
        PUBLICITY,
        //评委开出异常标记后，队长针对该作品的受限补交窗口
        SUPPLEMENT
    }
}
