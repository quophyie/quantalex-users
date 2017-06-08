package com.quantal.exchange.users.validators.password;

/**
 * Created by root on 05/06/2017.
 */
import com.quantal.exchange.users.enums.PasswordMatchType;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Documented
public @interface PasswordMatches {
    String message() default "Passwords don't match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    PasswordMatchType passwordMatchType() default PasswordMatchType.DISALLOW_NULL_MATCH;

}
