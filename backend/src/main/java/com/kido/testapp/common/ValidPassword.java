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

/** Bean Validation for {@link PasswordPolicy}. Use on every request field that sets a new password. */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPassword.Validator.class)
public @interface ValidPassword {

    String message() default PasswordPolicy.MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return PasswordPolicy.violation(value) == null;
        }
    }
}
