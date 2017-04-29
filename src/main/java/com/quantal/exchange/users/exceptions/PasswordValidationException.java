package com.quantal.exchange.users.exceptions;

/**
 * Created by dman on 24/04/2017.
 */
public class PasswordValidationException extends RuntimeException{
    public PasswordValidationException(String message){
        super(message);
    }
}
