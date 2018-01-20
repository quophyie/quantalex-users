package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Created by root on 08/06/2017.
 */
@RunWith( SpringRunner.class)
public class LoginFacadeTests {

    @Mock
    private OrikaBeanMapper orikaBeanMapper;

    @Mock
    private NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper;

    @Mock
    private UserService userService;

    @Mock
    private MessageService messageService;

    @Mock
    private LoginService loginService;

    private LoginFacade loginFacade;
    private LoginDto loginDto;

    @Before
    public void setUp() {
     this.loginFacade = new LoginFacade(orikaBeanMapper,
             nullSkippingOrikaBeanMapper,
             messageService,
             loginService);

        ReflectionTestUtils.setField(loginFacade, "logger", QuantalLoggerFactory.getLogger(LoginFacade.class, new CommonLogFields()));
        ReflectionTestUtils.setField(loginFacade, "taskExecutor", Executors.newSingleThreadExecutor());
        loginDto = new LoginDto();

    }

    @Test
    public void shouldReturn400BadRequestGivenNullLoginDto() throws ExecutionException, InterruptedException {

        String errMsg = "data provided cannot be null";
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED))
                .willReturn(errMsg);

        ResponseEntity responseEntity = loginFacade.login(null).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        String message = TestUtil.getResponseDtoMessage(responseEntity);

        assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);

    }


    @Test
    public void shouldReturn403UnauthorizedGivenInvalidEmail() throws ExecutionException, InterruptedException {

        String invalidUserErrMsg = "invalid email or password";
        String email = "notfoud@quantal.com";
        String password = "Password";

        given(messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD))
                .willReturn(invalidUserErrMsg);

        given(loginService.login(email, password))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new NotFoundException(""));
                    return future;
                });

        given(userService.findOneByEmail(email))
                .willAnswer(invocationOnMock -> null);

        loginDto.setEmail(email);
        loginDto.setPassword(password);

        ResponseEntity responseEntity = loginFacade.login(loginDto).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();

        assertThat(httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(messageService).getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
        verify(loginService).login(email, password);

    }

    @Test
    public void shouldReturn200GivenValidLoginDto() throws ExecutionException, InterruptedException {

        String jwt = "jwt_token";
        String password = "password";
        String email = "user@quantal.com";
        loginDto.setPassword(password);
        loginDto.setEmail(email);

        given(loginService.login(email, password))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(jwt));

        ResponseEntity responseEntity = loginFacade.login(loginDto).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        TokenDto tokenDto = TestUtil.getResponseDtoData(responseEntity);

        assertThat(httpStatus).isEqualTo(HttpStatus.OK);
        assertThat(tokenDto.getToken()).isEqualToIgnoringCase(jwt);

        verify(loginService).login(email, password);

    }


    @Test
    public void shouldReturn400GivenEmptyAuthorizatationHeader() throws ExecutionException, InterruptedException {

        String authHeader = "";
        String errMessage = "authorisation header data provided is empty or null";
        Long userId = 1L;

        String[] replacements = new String[]{"authorisation header"};
        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements))
                .willReturn(errMessage);

        ResponseEntity responseEntity = loginFacade.logout(userId, authHeader).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseDtoMsg = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(responseDtoMsg).isEqualToIgnoringCase(errMessage);
        verify(messageService).getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements);
    }

    @Test
    public void shouldReturn400GivenEmptyBearerTokenInAuthorizatationHeader() throws ExecutionException, InterruptedException {

        String jwt = "";
        String authHeader = "Bearer " + jwt;
        String errMessage = "authorization header bearer token data provided is empty or null";
        Long userId = 1L;

        String[] replacements = new String[]{"authorization header bearer token"};
        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements))
                .willReturn(errMessage);

        loginFacade.logout(userId, authHeader).get();
        verify(messageService).getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements);
    }

    @Test
    public void shouldReturn400GivenHandleIllegalArgumentExceptionOnLogout() throws ExecutionException, InterruptedException {

        String jwt = "jwt_token";
        String authHeader = "Bearer "+jwt;
        Long userId = 1L;

        String message = "data null or empty";

        given(loginService.logout(jwt))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new IllegalArgumentException(""));
                    return future;

                });

        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA))
                .willReturn(message);

        ResponseEntity responseEntity = loginFacade.logout(userId, authHeader).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();

        assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST);

        verify(loginService).logout(jwt);

    }

    @Test
    public void shouldLogoutAndReturn200GivenValidUserIdAndAuthHeader() throws ExecutionException, InterruptedException {

        String jwt = "jwt_token";
        String authHeader = "Bearer "+jwt;
        Long userId = 1L;

        String message = "OK";

        given(loginService.logout(jwt))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(null));

        given(messageService.getMessage(MessageCodes.SUCCESS))
                .willReturn(message);

        ResponseEntity responseEntity = loginFacade.logout(userId, authHeader).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        String responseDataMsg = TestUtil.getResponseDtoMessage(responseEntity);

        assertThat(httpStatus).isEqualTo(HttpStatus.OK);
        assertThat(responseDataMsg).isEqualToIgnoringCase(message);

        verify(loginService).logout(jwt);

    }





}
