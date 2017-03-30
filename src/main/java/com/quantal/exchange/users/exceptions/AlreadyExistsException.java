package com.quantal.exchange.users.exceptions;

/**
 * Created by dman on 29/03/2017.
 */
public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String message){
        super(message);
    }
}
