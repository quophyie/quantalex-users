package com.quantal.exchange.users.services.interfaces;

import org.passay.PasswordValidator;
import org.passay.RuleResult;

/**
 * Created by dman on 24/04/2017.
 */
public interface PasswordService {

    /**
     *
     * Hashes a plain text password
     * @param plainTextPassword - The plain text password to hash
     * //@param salt [optional]  - If provided, will be used as the salt used to hash the password
     * @return The hashed password
     */
    String hashPassword(String plainTextPassword/*, String salt*/);


    /**
     * Checks whether the provided password matches the hash
     * @param plainTextPassword
     * @param hashedPassword
     * @return
     */
    boolean checkPassword(String plainTextPassword, String hashedPassword);

    /**
     * Checks whether the provided plain text password matches validation rules
     * @param plainTextPassword - the plain text password
     * @return
     */
    RuleResult checkPasswordValidity(String plainTextPassword);

    /**
     * Returns the password validator
     * @return
     */
    PasswordValidator getPasswordValidator();

}
