package com.quantal.exchange.users.validators;

import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.validators.password.PasswordMatchesValidator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by root on 06/06/2017.
 */
public class PasswordMatchesValidatorTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private PasswordMatchesValidator passwordMatchesValidator;
    private UserDto userDto;
    private String password = "password";


    @Before
    public void setUp() {
        userDto = new UserDto();
        userDto.setPassword(password);
        passwordMatchesValidator = new PasswordMatchesValidator();
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullUserDto() {
        String errMsg = "UserDto cannot be null";
        userDto = null;
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(errMsg);
        passwordMatchesValidator.isValid(userDto, null);
    }

    @Test
    public void shouldNotMatchGivenNullPassword() {
        userDto.setPassword(null);
        boolean result  = passwordMatchesValidator.isValid(userDto, null);
        assertThat(result).isFalse();
    }

    @Test
    public void shouldMatchGivenMatchingPasswords() {
        userDto.setPassword("password");
        userDto.setConfirmedPassword("password");
        boolean result  = passwordMatchesValidator.isValid(userDto, null);
        assertThat(result).isTrue();
    }

    @Test
    public void shouldNotMatchGivenNonMatchingPasswords() {
        userDto.setPassword("password");
        userDto.setConfirmedPassword("password1");
        boolean result  = passwordMatchesValidator.isValid(userDto, null);
        assertThat(result).isFalse();
    }
}
