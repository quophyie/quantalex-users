package com.quantal.exchange.users.services.implementations;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.shared.services.interfaces.MessageService;
import org.apache.commons.lang3.StringUtils;
import org.passay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by dman on 24/04/2017.
 */
@Service
public class PasswordServiceImpl implements PasswordService{

    private MessageService messageService;
    private PasswordValidator validator;
    private final String PASSORD_SALT;

    @Autowired
    public PasswordServiceImpl(MessageService messageService, @Value("#{environment.PASSWORD_SALT}") String salt) {

        if (StringUtils.isEmpty(salt)){
            throw new IllegalArgumentException(messageService.getMessage(MessageCodes.INVALID_PASSWORD_SALT));
        }

        validator = new PasswordValidator(Arrays.asList(
                // length between 6 and 30 characters
                new LengthRule(6, 30),

                // at least one upper-case character
                new CharacterRule(EnglishCharacterData.UpperCase, 1),

                // at least one lower-case character
                new CharacterRule(EnglishCharacterData.LowerCase, 1),

                // at least one digit character
                new CharacterRule(EnglishCharacterData.Digit, 1),

                // at least one symbol (special character)
                new CharacterRule(EnglishCharacterData.Special, 1),

                // no whitespace
                new WhitespaceRule()));

        this.messageService = messageService;
        this.PASSORD_SALT = salt;
    }
    @Override
    public String hashPassword(String plainTextPassword) {

        if (StringUtils.isEmpty(plainTextPassword)) {
            String errMsg = messageService.getMessage(MessageCodes.INVALID_PLAIN_TEXT_PASSWORD);
            throw new IllegalArgumentException(errMsg);
        }
        return BCrypt.hashpw(plainTextPassword, PASSORD_SALT);
    }

    @Override
    public boolean checkPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }

    @Override
    public RuleResult checkPasswordValidity(String plainTextPassword) {

        RuleResult result =  validator.validate(new PasswordData(new String(plainTextPassword)));

        return result;
    }

    @Override
    public PasswordValidator getPasswordValidator() {
        return this.validator;
    }

    @Override
    public String getPasswordValidationCheckErrorMessages(RuleResult ruleResult, String separator){
        if(ruleResult == null )
            return "";
        String delimiter = StringUtils.isEmpty(separator) ? "" : separator;

        String message = this
                .getPasswordValidator()
                .getMessages(ruleResult)
                .stream()
                .reduce((s, s2) -> s + delimiter + s2).orElse("");

        return message;

    }
}
