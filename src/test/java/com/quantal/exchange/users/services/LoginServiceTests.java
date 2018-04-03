package com.quantal.exchange.users.services;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialsListResponseDto;
import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.AuthResponseDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.exceptions.InvalidDataException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.implementations.LoginServiceImpl;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.quantal.exchange.users.constants.TestConstants.EVENT;
import static com.quantal.exchange.users.constants.TestConstants.TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


/**
 * Created by root on 09/06/2017.
 */
@RunWith(SpringRunner.class)
public class LoginServiceTests {

    private LoginService loginService;



    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private MessageService messageService;

    @Mock
    private ApiGatewayService apiGatewayService;

    @Mock
    private AuthorizationApiService authorizationApiService;

    @Mock
    private MDCAdapter mdcAdapter;

    private User user;

    private  String email = "testemail@quantal.com";
    String password = "Password";
    String hashedPassword = "hashedPassword";


    @Before
    public void setUp() {
        loginService = new LoginServiceImpl(userService, passwordService, messageService, apiGatewayService, authorizationApiService/*, new CommonLogFields(), null*/);
        user = UserTestUtil.createUserModel
                (1L,
                        "testfirstname",
                        "testlastname",
                        email,
                        hashedPassword,
                        GenderEnum.M,
                        LocalDate.of(1981, 01, 01)

                        );
        given(mdcAdapter.get(CommonConstants.TRACE_ID_MDC_KEY)).willReturn(TRACE_ID);
        given(mdcAdapter.get(CommonConstants.EVENT_KEY)).willReturn(EVENT);
        QuantalLogger quantalLogger = QuantalLoggerFactory.getLogger(UserManagementFacade.class,  LoggerConfig.builder().commonLogFields(new CommonLogFields()).build());
        quantalLogger = (QuantalLogger) quantalLogger.with(CommonConstants.TRACE_ID_MDC_KEY, TRACE_ID).with(CommonConstants.EVENT_KEY, "EVENT");
        ReflectionTestUtils.setField(loginService, "logger", quantalLogger);
        ReflectionTestUtils.setField(loginService, "taskExecutor", Executors.newSingleThreadExecutor());
        ReflectionTestUtils.setField(loginService, "JWT_SECRET", "secret");
    }

    @Test
    public void shouldThrowNotFoundExceptionGivenNonExistentEmail() throws ExecutionException, InterruptedException {
        String email = "notfoud@quantal.com";
        String password = "Password";
        String errMessage = "user not found";
        String[] replacements = new String[]{User.class.getSimpleName()};
        given(messageService.getMessage(MessageCodes.NOT_FOUND, replacements))
                .willReturn(errMessage);
        given(userService.findOneByEmail(email, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(null));

        try {
            loginService.login(email, password, mdcAdapter).get();
        } catch (Exception ex){
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(NotFoundException.class);

            verify(messageService).getMessage(MessageCodes.NOT_FOUND, replacements);
            verify(userService).findOneByEmail(email, mdcAdapter);

        }
    }

    @Test
    public void shouldThrowPasswordValidationExceptionGivenInvalidPassword() throws ExecutionException, InterruptedException {
        String invalidUserErrMsg = "invalid to or password";
        String email = "notfoud@quantal.com";
        String password = "Password";
        given(userService.findOneByEmail(email, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(user));


        given(passwordService.checkPassword(password, hashedPassword)).willReturn(false);

        try {
            loginService.login(email, password, mdcAdapter).get();
        } catch (Exception ex){
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(PasswordValidationException.class);
            verify(userService).findOneByEmail(email, mdcAdapter);
            verify(passwordService).checkPassword(password, hashedPassword);

        }
    }

    @Test
    public void shouldReturnJwtGivenValidEmailAndPassword() throws ExecutionException, InterruptedException {

        String jwt = "jwt_token";
        AuthRequestDto authRequestDto = new AuthRequestDto();
        authRequestDto.setEmail(email);
        given(userService.findOneByEmail(email, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(user));

        given(passwordService.checkPassword(password, hashedPassword)).willReturn(true);
       given( authorizationApiService.requestToken(authRequestDto, EVENT, TRACE_ID))
               .willAnswer(invocationOnMock -> {
                   TokenDto tokenDto = new TokenDto();
                   tokenDto.setToken(jwt);
                   return CompletableFuture.completedFuture(tokenDto);
               });

        String result = loginService.login(email, password, mdcAdapter).get();
        assertThat(result).isEqualToIgnoringCase(jwt);
        verify(passwordService).checkPassword(password, hashedPassword);
        verify(userService).findOneByEmail(email, mdcAdapter);
        verify(authorizationApiService).requestToken(authRequestDto, EVENT, TRACE_ID);

    }

    @Test
    @Ignore
    public void shouldThrowInvalidDataExceptionGivenEmptyCredentialKey() throws ExecutionException, InterruptedException {

        String apiJwtCredentuakKey = null;
        given(userService.findOneByEmail(email, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(user));


        given(passwordService.checkPassword(password, hashedPassword)).willReturn(true);

        given(apiGatewayService.getConsumerJwtCredentials(email, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)))
                .willAnswer(invocationOnMock -> {
                    ApiJwtUserCredentialsListResponseDto jwtUserCredentialListResponseDto = new ApiJwtUserCredentialsListResponseDto();
                    ApiJwtUserCredentialResponseDto jwtUserCredentialResponseDto = new ApiJwtUserCredentialResponseDto();
                    jwtUserCredentialResponseDto.setKey(apiJwtCredentuakKey);
                    jwtUserCredentialListResponseDto.setData(Arrays.asList(jwtUserCredentialResponseDto));
                    return CompletableFuture.completedFuture(jwtUserCredentialListResponseDto);
                });
        try {
            loginService.login(email, password, mdcAdapter).get();
        } catch (Exception ex) {
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(InvalidDataException.class);
            verify(passwordService).checkPassword(password, hashedPassword);
            verify(userService).findOneByEmail(email, mdcAdapter);
            verify(apiGatewayService).getConsumerJwtCredentials(email, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY));
        }


    }

    @Test
    public void shouldThrowIllegalArgumentExceptionGivenNullJwt(){
        String jwt = null;
        String message = "data provided cannot be null";

        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED))
                .willReturn(message);

        try {
            loginService.logout(jwt).get();
        } catch (Exception ex){
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(IllegalArgumentException.class);
            verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionGivenEmptyJwt(){
        String jwt = "";
        String message = "data provided cannot be null";

        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED))
                .willReturn(message);

        try {
            loginService.logout(jwt).get();
        } catch (Exception ex){
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(IllegalArgumentException.class);
            verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);
        }
    }


    @Test
    public void shouldThrowIllegalArgumentExceptionGivenEmptyJwtContainingEmptyJti(){
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        String message = "jti data provided is empty or null";

        String[] replacements = new String[]{"jti"};
        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements))
                .willReturn(message);

        try {
            //java.lang.IllegalArgumentException: signing key cannot be null or empty.
            loginService.logout(jwt).get();
        } catch (Exception ex){
            RuntimeException busEx = CommonUtils.extractBusinessException(ex);
            assertThat(busEx).isInstanceOf(IllegalArgumentException.class);
            verify(messageService).getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements);
        }
    }

    @Test
    public void shouldLogoutUserGivenValidToken() throws ExecutionException, InterruptedException {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIxMjM0NTY3ODkwIn0.A3-qkjWnE3Y_8Hc9TlP_MIe9OWAXCy3TCsx-e1V40gc";
        String jti = "1234567890";

        given(authorizationApiService.deleteToken(jti, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)))
                .willAnswer(invocationOnMock -> {
                    AuthResponseDto authResponseDto = new AuthResponseDto();
                    authResponseDto.setCode(200);
                    return CompletableFuture.completedFuture(authResponseDto);
                });

        Object result = loginService.logout(jwt).get();
        assertThat(result).isNull();

        verify(authorizationApiService).deleteToken(jti, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY));
    }

}
