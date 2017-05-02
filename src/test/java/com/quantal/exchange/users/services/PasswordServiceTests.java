package com.quantal.exchange.users.services;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.services.implementations.PasswordServiceImpl;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.shared.services.interfaces.MessageService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Created by dman on 24/04/2017.
 */
@RunWith(SpringRunner.class)
public class PasswordServiceTests {

    private PasswordService passwordService;

    private PasswordValidator passwordValidator;

    private String plainTextPassword = "password";

    private final String salt = "$2a$10$ZpK.XFLeMPjsvwvFKx/CeO";

    @MockBean
    private MessageService messageService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp(){
      passwordService = new PasswordServiceImpl(messageService, salt);
      passwordValidator = passwordService.getPasswordValidator();
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionGivenNullSalt() {

        String errMsg = "invalid salt provided";

        //Then
        thrown.expectMessage(errMsg);
        thrown.expect(IllegalArgumentException.class);

        //Given
        given(messageService.getMessage(MessageCodes.INVALID_PASSWORD_SALT))
                .willReturn(errMsg);

        //When
        new PasswordServiceImpl(messageService, null);

        verify(messageService).getMessage(MessageCodes.INVALID_PASSWORD_SALT);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionGivenEmptySalt() {

        String errMsg = "invalid salt provided";

        //Then
        thrown.expectMessage(errMsg);
        thrown.expect(IllegalArgumentException.class);

        //Given
        given(messageService.getMessage(MessageCodes.INVALID_PASSWORD_SALT))
                .willReturn(errMsg);

        //When
        new PasswordServiceImpl(messageService, "");

        verify(messageService).getMessage(MessageCodes.INVALID_PASSWORD_SALT);
    }


    @Test
    public void shouldThrowIllegalArgumentExceptionGivenNullPlainTextPassword() {

        String errMsg = "plain text password is required and cannot be null or empty";

        //Then
        thrown.expectMessage(errMsg);
        thrown.expect(IllegalArgumentException.class);

        //Given
        given(messageService.getMessage(MessageCodes.INVALID_PLAIN_TEXT_PASSWORD))
                .willReturn(errMsg);

        //When
        passwordService.hashPassword(null);

        verify(passwordService).hashPassword(null);
    }

    @Test
    public void shouldGenerateHashedPasswordGivenPlainTextPassword() {

        //When
        String hashedPassword = passwordService.hashPassword(plainTextPassword);

        assertThat(hashedPassword).isNotBlank();

    }



    @Test
    public void shouldCheckPasswordAgainstHashAndReturnTrueGivenHashOfPassword() {

        String hashedPassword = "$2a$10$ZpK.XFLeMPjsvwvFKx/CeOL.lncdO4vSyHh/MI3xl0I/2uIs8wSu.";
        boolean result = passwordService.checkPassword(plainTextPassword, hashedPassword);

        assertThat(result).isTrue();
    }

    @Test
    public void shouldCheckPasswordAgainstHashAndReturnFalseGivenHashOfPassword() {

        String hashedPassword = "$2a$10$ZpK.XFLeMPjsvwvFKx/CeOL.lncdO4vSyHh/MI3xl0I/2uIs8wSu6";
        boolean result = passwordService.checkPassword(plainTextPassword, hashedPassword);

        assertThat(result).isFalse();
    }
@Test
    public void shouldReturnErrorMessageAboutPasswordLengthGivenPasswordWhichLessEightInLength(){

        RuleResult result = passwordService.checkPasswordValidity("pass");
        List<String> messages = passwordValidator.getMessages(result);
        assertThat(messages.get(0)).isEqualToIgnoringCase("Password must be at least 6 characters in length.");
    }

    @Test
    public void shouldReturnErrorMessagAboutNumberOfUpperCaseCharactersGivenPasswordWithoutUppercaseCharacters(){

        RuleResult result = passwordService.checkPasswordValidity("password");
        List<String> messages = passwordValidator.getMessages(result);
        assertThat(messages.get(0)).isEqualToIgnoringCase("Password must contain at least 1 uppercase characters.");
    }

    @Test
    public void shouldReturnErrorMessagAboutNumberOfDigitsCharactersGivenPasswordWithoutDigits(){

        RuleResult result = passwordService.checkPasswordValidity("Password");
        List<String> messages = passwordValidator.getMessages(result);
        assertThat(messages.get(0)).isEqualToIgnoringCase("Password must contain at least 1 digit characters.");
    }

    @Test
    public void shouldReturnErrorMessagAboutNumberOfSpecialCharactersGivenPasswordWithoutSpecialCharacters(){

        RuleResult result = passwordService.checkPasswordValidity("Password1");
        List<String> messages = passwordValidator.getMessages(result);
        assertThat(messages.get(0)).isEqualToIgnoringCase("Password must contain at least 1 special characters.");
    }

    @Test
    public void shouldReturnErrorMessageSayingPasswordCannotContainSpacesGivenPasswordContainingSpaces(){

        RuleResult result = passwordService.checkPasswordValidity("Pass word1@");
        List<String> messages = passwordValidator.getMessages(result);
        assertThat(messages.get(0)).isEqualToIgnoringCase("Password cannot contain whitespace characters.");
    }

    @Test
    public void shouldReturnTrueGivenAValidPassword(){

        RuleResult result = passwordService.checkPasswordValidity("Password1@");
        assertThat(result.isValid()).isTrue();
    }
}
