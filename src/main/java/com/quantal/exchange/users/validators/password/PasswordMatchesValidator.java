package com.quantal.exchange.users.validators.password;

import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.PasswordMatchType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Created by root on 05/06/2017.
 */

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    //private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final org.apache.logging.log4j.Logger logger =  LogManager.getLogger();
    private PasswordMatches constraintAnnotation;
    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }


    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context){
        if (obj == null)
            throw logger.throwing(new NullPointerException("UserDto cannot be null"));
        UserDto user = (UserDto) obj;

        if (this.constraintAnnotation!=null &&
                this.constraintAnnotation.passwordMatchType() == PasswordMatchType.ALLOW_NULL_MATCH
                && StringUtils.isEmpty(user.getPassword()))
            return true;

        return !StringUtils.isEmpty(user.getPassword()) && user.getPassword().equals(user.getConfirmedPassword());
    }
}
