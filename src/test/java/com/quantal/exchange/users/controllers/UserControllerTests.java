package com.quantal.exchange.users.controllers;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.controlleradvice.ExceptionHandlerControllerAdvice;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.dto.ResponseMessageDto;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.quantal.exchange.users.constants.TestConstants.EVENT;
import static com.quantal.exchange.users.constants.TestConstants.TRACE_ID;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_MDC_KEY;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dman on 24/03/2017.
 */

@RunWith(SpringRunner.class)
//@WebMvcTest(value = UserController.class, secure = false)
//@ContextConfiguration(classes={WebStartupConfig.class, WebSecurityConfig.class})
//@DataJpaTest
//@SpringBootTest(/*webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT/*, classes = UsersApplication.class*/)
public class UserControllerTests {

    private String persistedUserFirstName = "updatedUserFirstName";
    private String persistedUserLasttName = "updatedUserLasttName";
    private String persistedUserEmail = "persistedUserEmail@quantal.com";
    private String persistedUserPassword = "persistedUserPassword";
    private String persistedConfirmedUserPassword = "persistedUserPassword";
    private LocalDate persistedUserDob = LocalDate.of(1980, 1, 1);
    private GenderEnum persistedUserGenderEnum = GenderEnum.M;

    private Long userId = 1L;
   // @Autowired
    private MockMvc mvc;

    private UserController userController;

    @MockBean
    private UserService userService;

    @MockBean
    private UserManagementFacade userManagementFacade;

    @MockBean
    private MessageService messageService;

    @Mock
    private MDCAdapter mdcAdapter;

    @MockBean
    private ExecutorService taskExecutor;

    @Before
    public void setUp() {

        userController = new UserController(userManagementFacade, null);
        given(mdcAdapter.get(TRACE_ID_MDC_KEY)).willReturn(TRACE_ID);
        given(mdcAdapter.get(CommonConstants.EVENT_KEY)).willReturn(EVENT);

        MDC.put(TRACE_ID_MDC_KEY, TRACE_ID);
        MDC.put(EVENT_KEY, TRACE_ID);

        QuantalLogger quantalLogger = QuantalLoggerFactory.getLogger(UserManagementFacade.class,  LoggerConfig.builder().commonLogFields(new CommonLogFields()).build());
        quantalLogger = (QuantalLogger) quantalLogger.with(TRACE_ID_MDC_KEY, TRACE_ID).with(CommonConstants.EVENT_KEY, "EVENT");

        ReflectionTestUtils.setField(userController, "logger", quantalLogger);
        ExceptionHandlerControllerAdvice exceptionHandlerControllerAdvice = new ExceptionHandlerControllerAdvice(messageService);
        ReflectionTestUtils.setField(exceptionHandlerControllerAdvice, "logger", quantalLogger);

        mvc =  MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(exceptionHandlerControllerAdvice).build();
    }

    @Test
    public void shouldGetGiphyGivenQuery() throws Exception {

        String jsonResult = "{\"data\":[{\"type\":\"gif\",\"id\":\"jTnGaiuxvvDNK\",\"slug\":\"funny-cat-jTnGaiuxvvDNK\",\"url\":\"http:\\/\\/giphy.com\\/gifs\\/funny-cat-jTnGaiuxvvDNK\",\"bitly_gif_url\":\"http:\\/\\/gph.is\\/2cKVPVQ\"}]}";
        given(this.userManagementFacade.getFunnyCat())
                .willReturn(CompletableFuture.completedFuture(jsonResult));

        MvcResult asyscResult = this.mvc.perform(get("/users/").accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        mvc.perform(asyncDispatch(asyscResult))
            .andExpect(status().isOk())
            .andExpect(
                json()
                    .node("data")
                    .isArray())
            .andExpect(
                json()
                    .node("data")
                    .matches(hasItem(jsonPartMatches("type", equalTo("gif")))));

        verify(userManagementFacade).getFunnyCat();


    }

    @Test
    public void shouldUpdateAUser() throws Exception {

        String updatedUserFirstName = "updatedUserFirstName";
        String updatedUserLastName = "updatedUserLastName";
        Long userId = 1L;
        UserDto updateData = UserTestUtil.createApiGatewayUserDto(userId,
                updatedUserFirstName,
                updatedUserLastName,
                null,
                null,
                null,
                null);

        UserDto updatedUser = UserTestUtil.createApiGatewayUserDto(userId,
                updatedUserFirstName,
                updatedUserLastName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGenderEnum,
                persistedUserDob);

        ResponseEntity response = new ResponseEntity(updatedUser, HttpStatus.OK);

        //given(this.userManagementFacade.updateUser(userId,updateData))
        //        .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(response));

        given(this.userManagementFacade.updateUser(userId,updateData))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(response));


        MvcResult asyncResult = this.mvc.perform(put("/users/{id}", new Object[]{userId})

                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(updateData)))
                .andReturn();

        mvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(
                        json()
                        .isObject())
              .andExpect(
                        json()
                                .node("firstName")
                                .isEqualTo(updatedUserFirstName))
                .andExpect(
                        json()
                                .node("lastName")
                                .isEqualTo(updatedUserLastName))
                .andExpect(
                        json()
                                .node("email")
                                .isEqualTo(persistedUserEmail))
                .andExpect(
                        json()
                                .node("gender")
                                .isEqualTo(persistedUserGenderEnum));

        verify(userManagementFacade).updateUser(userId,updateData);
    }

    @Test
    public void shouldFindAUserGivenTheUserId() throws Exception {

        UserDto userDto = UserTestUtil.createApiGatewayUserDto(userId,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGenderEnum,
                persistedUserDob);

        ResponseEntity response = new ResponseEntity(userDto, HttpStatus.OK);

        given(this.userManagementFacade.findUserById(userId))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(response));


        MvcResult asyncResult = this.mvc
                .perform(get("/users/{id}", new Object[]{userId})
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        mvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("firstName")
                                .isEqualTo(persistedUserFirstName))
                .andExpect(
                        json()
                                .node("lastName")
                                .isEqualTo(persistedUserLasttName))
                .andExpect(
                        json()
                                .node("email")
                                .isEqualTo(persistedUserEmail))
                .andExpect(
                        json()
                                .node("gender")
                                .isEqualTo(persistedUserGenderEnum));

        verify(userManagementFacade).findUserById(userId);
    }



    @Test
    public void shouldDeleteAUserGivenTheUserId() throws Exception {

        ResponseEntity response = new ResponseEntity(new ResponseMessageDto("OK", 200), HttpStatus.OK);

        given(this.userManagementFacade.deleteByUserId(userId))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(response));


        MvcResult asyncResult = this
                .mvc
                .perform(delete("/users/{id}", new Object[]{userId})
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        mvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("message")
                                .isEqualTo("OK"));
        verify(userManagementFacade).deleteByUserId(userId);
    }

    @Test
    public void shouldCreateANewUserGivenUserData() throws Exception {

        UserDto userDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGenderEnum,
                null);
        userDto.setConfirmedPassword(persistedConfirmedUserPassword);

        UserDto createdUserDto = UserTestUtil.createApiGatewayUserDto(userId,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGenderEnum,
                null);

        ResponseEntity response = new ResponseEntity(createdUserDto, HttpStatus.OK);

        given(this.userManagementFacade.save(userDto, MDC.getMDCAdapter()))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture completableFuture = new CompletableFuture();
                    completableFuture.complete(response);
                    return completableFuture;
                });

        MvcResult asyscResult = this.mvc.perform(post("/users/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(userDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
        /*this.mvc.perform(
                  post("/users/")
                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(TestUtil.convertObjectToJsonString(userDto))
                )*/

                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("id")
                                .isEqualTo(userId))
                .andExpect(
                        json()
                                .node("firstName")
                                .isEqualTo(persistedUserFirstName))
                .andExpect(
                        json()
                                .node("lastName")
                                .isEqualTo(persistedUserLasttName))
                .andExpect(
                        json()
                                .node("email")
                                .isEqualTo(persistedUserEmail))
                .andExpect(
                        json()
                                .node("gender")
                                .isEqualTo(persistedUserGenderEnum));

        verify(userManagementFacade).save(userDto, MDC.getMDCAdapter());
    }

    @Test
    public void shouldSendPasswordResetEmailGivenUserEmail() throws Exception {

        UserDto userDto = new UserDto();
        userDto.setEmail("user@quanta.com");


        ResponseEntity response = new ResponseEntity(new ResponseMessageDto("OK", 200), HttpStatus.OK);


        given(this.userManagementFacade.requestPasswordReset(userDto.getEmail(), MDC.getMDCAdapter()))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture completableFuture = new CompletableFuture();
                    completableFuture.complete(response);
                    return completableFuture;
                });

        MvcResult asyncResult = this.mvc.perform(post("/users/forgotten-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(userDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("message")
                                .isEqualTo("OK"));
        verify(userManagementFacade).requestPasswordReset(userDto.getEmail(), MDC.getMDCAdapter());
    }


    @Test
    public void shouldResetPasswordGivenNewPassword() throws Exception {

        UserDto userDto = new UserDto();
        userDto.setConfirmedPassword("newPassword");
        userDto.setPassword("newPassword");
        userDto.setEmail("user@quanta.com");

        String jwt = "jwt_token";
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(jwt);

        ResponseEntity response = new ResponseEntity(tokenDto, HttpStatus.OK);

        given(this.userManagementFacade.resetPassword(userDto, MDC.getMDCAdapter()))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture completableFuture = new CompletableFuture();
                    completableFuture.complete(response);
                    return completableFuture;
                });

        MvcResult asyncResult = this.mvc.perform(post("/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(userDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("token")
                                .isEqualTo(tokenDto.getToken()));

        verify(userManagementFacade).resetPassword(userDto, MDC.getMDCAdapter());
    }

    @Test
    public void shouldReturn400BadRequestByControllerAdviceWhenNullPointerExceptionThrown() throws Exception {

        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = "data provided cannot be null";
        UserDto createdUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGenderEnum,
                null);
        createdUserDto.setConfirmedPassword(persistedUserPassword);

        given(userManagementFacade.save(createdUserDto, MDC.getMDCAdapter()))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture completableFuture = new CompletableFuture();
                    completableFuture.completeExceptionally(new NullPointerException(errMsg));
                    return completableFuture;
                });

        MvcResult asyscResult = this.mvc.perform(post("/users/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(createdUserDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isBadRequest());
        verify(userManagementFacade, times(1)).save(createdUserDto, MDC.getMDCAdapter());
    }

    @Test
    public void shouldReturn409ConflictGivenAUserThatAlreadyExists() throws Exception {

        String persistedModelFirstName = "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = String.format("user with to %s already exists", persistedModelEmail);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, null);
        createUserDto.setConfirmedPassword(persistedModelPassword);

        given(this.userManagementFacade
                .save(createUserDto, MDC.getMDCAdapter()))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new AlreadyExistsException(errMsg));
                    return future;
                });

        String content = TestUtil.convertObjectToJsonString(createUserDto);
        MvcResult asyscResult = this.mvc.perform(post("/users/")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(content))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isConflict());

        verify(userManagementFacade, times(1)).save(createUserDto, MDC.getMDCAdapter());
    }

    @Test
    public void shouldReturn400BadRequestGivenInvalidPasswordOnPasswordReset () throws Exception {


        UserDto userDto = new UserDto();
        userDto.setEmail(persistedUserEmail);
        userDto.setPassword(persistedUserPassword);
        userDto.setConfirmedPassword(persistedConfirmedUserPassword);


        given(userManagementFacade.resetPassword(userDto, MDC.getMDCAdapter() )).willAnswer(invocation -> {
            CompletableFuture completableFuture = new CompletableFuture();
            completableFuture.completeExceptionally(new PasswordValidationException("password exception"));
            return completableFuture;
        });
        String content = TestUtil.convertObjectToJsonString(userDto);
        MvcResult asyscResult = this.mvc.perform(post("/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(content))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isBadRequest());

        verify(userManagementFacade).resetPassword(userDto, MDC.getMDCAdapter() );


    }

    @Test
    public void should404NotFoundGiveninvalidUserIdOnDelete() throws Exception {
        String errMsg = "User not found";

        Long userToDelId = 2L;
        given(messageService.getMessage(MessageCodes.NOT_FOUND, new String[] {User.class.getSimpleName()})).willReturn(errMsg);

        given(userManagementFacade.deleteByUserId(userToDelId)).willAnswer(invocationOnMock -> {
            CompletableFuture<Void> future = new CompletableFuture();
            future.completeExceptionally(new NotFoundException(""));
            return future;
        });


        MvcResult asyscResult = this.mvc.perform(delete("/users/{userId}", userToDelId)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isNotFound());

        verify(userManagementFacade).deleteByUserId(userToDelId);

    }


    @After
    public void tearDown() {

        userManagementFacade = null;
    }


}
