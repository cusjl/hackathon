package org.hackathon.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hackathon.annotation.PatchNotBlank;

public class PatchNotBlankValidator implements ConstraintValidator<PatchNotBlank, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !value.isBlank();
    }
}