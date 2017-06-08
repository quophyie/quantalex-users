package com.quantal.exchange.users.validators;

import com.quantal.exchange.users.validators.email.EmailValidator;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Created by root on 08/06/2017.
 */
public class EmailValidatorTests {

    private EmailValidator emailValidator;

    @Before
    public void setUp() {
        emailValidator = new EmailValidator();
    }

    @Test
    public void shouldReturnFalseGivenEmptyEmail() {
      boolean result = emailValidator.isValid("", null);
      assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseGivenNullEmail() {
        boolean result = emailValidator.isValid(null, null);
        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnFalseGivenInvalideEmail() {
        boolean result = emailValidator.isValid("george", null);
        assertThat(result).isFalse();
    }

    @Test
    public void shouldReturnTrueGivenValidEmail() {
        boolean result = emailValidator.isValid("george@quantal.com", null);
        assertThat(result).isTrue();
    }
}
