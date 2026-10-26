package com.kido.testapp.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Length limit as stored: the trimmed value ({@link Text#trim}), counted in code points — the same unit
 * as Postgres varchar(n). (@Size counts UTF-16 units of the raw value, so an emoji counts twice and
 * padding counts.) Null is valid; combine with @NotNull when the field is required.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxLength.Validator.class)
public @interface MaxLength {

    int value();

    String message() default "must be at most {value} characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MaxLength, String> {
        private int max;

        @Override
        public void initialize(MaxLength annotation) {
            max = annotation.value();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            String trimmed = Text.trim(value);
            return trimmed.codePointCount(0, trimmed.length()) <= max;
        }
    }
}
