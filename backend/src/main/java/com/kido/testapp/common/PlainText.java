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

/** No control characters ({@link Text#hasControlChars}). Use multiline = true for fields like bio. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlainText.Validator.class)
public @interface PlainText {

    boolean multiline() default false;

    String message() default "must not contain control characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PlainText, String> {
        private boolean multiline;

        @Override
        public void initialize(PlainText annotation) {
            multiline = annotation.multiline();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return !Text.hasControlChars(value, multiline);
        }
    }
}
