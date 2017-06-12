package com.quantal.exchange.users.exceptions;

/**
 * Created by dman on 29/03/2017.
 */
public class InvalidResponseException extends RuntimeException {

    public InvalidResponseException(String message){
        super(message);
    }
}
