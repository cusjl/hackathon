package org.hackathon.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.hackathon.validator.PatchNotBlankValidator;

import java.lang.annotation.*;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PatchNotBlankValidator.class)
@Documented
public @interface PatchNotBlank {
    String message() default "字段不能为空";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}