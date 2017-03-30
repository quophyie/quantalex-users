package com.quantal.exchange.users.exceptions;

/**
 * Created by dman on 29/03/2017.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message){
        super(message);
    }
}
