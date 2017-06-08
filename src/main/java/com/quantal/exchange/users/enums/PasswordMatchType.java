package com.quantal.exchange.users.enums;

/**
 * Created by root on 06/06/2017.
 */
public enum PasswordMatchType {
    /**
     * Will allow the PasswordMatches annotation to return true (i.e. the password and the confirmed
     * match if the password is set to null)
     */
    ALLOW_NULL_MATCH,

    /**
     * Will allow the PasswordMatches annotation to return false (i.e. the password and the confirmed
     * do not match if the password is set to null)
     */
    DISALLOW_NULL_MATCH

}
