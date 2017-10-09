package com.quantal.exchange.users.controllers;

import com.quantal.exchange.users.controlleradvice.ExceptionHandlerCotrollerAdvice;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import com.quantal.shared.dto.ResponseMessageDto;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.shared.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dman on 24/03/2017.
 */

@RunWith(SpringRunner.class)
@WebMvcTest(value = UserController.class, secure = false)
//@ContextConfiguration(classes={WebStartupConfig.class, WebSecurityConfig.class})
//@DataJpaTest
//@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT/*, classes = UsersApplication.class*/)
public class UserControllerTests {

    private String persistedUserFirstName = "updatedUserFirstName";
    private String persistedUserLasttName = "updatedUserLasttName";
    private String persistedUserEmail = "persistedUserEmail@quantal.com";
    private String persistedUserPassword = "persistedUserPassword";
    private String persistedConfirmedUserPassword = "persistedUserPassword";
    private LocalDate persistedUserDob = LocalDate.of(1980, 1, 1);
    private Gender persistedUserGender = Gender.M;

    private Long userId = 1L;
    @Autowired
    private MockMvc mvc;

    private UserController userController;

    @MockBean
    private UserService userService;

    @MockBean
    private UserManagementFacade userManagementFacade;

    @MockBean
    private MessageService messageService;



    @Before
    public void setUp() {

        userController = new UserController(userManagementFacade, null);
        mvc=  MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new ExceptionHandlerCotrollerAdvice(messageService)).build();
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
                persistedUserGender,
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
                                .isEqualTo(persistedUserGender));

        verify(userManagementFacade).updateUser(userId,updateData);
    }

    @Test
    public void shouldFindAUserGivenTheUserId() throws Exception {

        UserDto userDto = UserTestUtil.createApiGatewayUserDto(userId,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGender,
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
                                .isEqualTo(persistedUserGender));

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
                persistedUserGender,
                null);
        userDto.setConfirmedPassword(persistedConfirmedUserPassword);

        UserDto createdUserDto = UserTestUtil.createApiGatewayUserDto(userId,
                persistedUserFirstName,
                persistedUserLasttName,
                persistedUserEmail,
                persistedUserPassword,
                persistedUserGender,
                null);

        ResponseEntity response = new ResponseEntity(createdUserDto, HttpStatus.OK);

        given(this.userManagementFacade.save(userDto))
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
                                .isEqualTo(persistedUserGender));

        verify(userManagementFacade).save(userDto);
    }

    @Test
    public void shouldSendPasswordResetEmailGivenUserEmail() throws Exception {

        UserDto userDto = new UserDto();
        userDto.setEmail("user@quanta.com");


        ResponseEntity response = new ResponseEntity(new ResponseMessageDto("OK", 200), HttpStatus.OK);

        given(this.userManagementFacade.requestPasswordReset(userDto.getEmail()))
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
        verify(userManagementFacade).requestPasswordReset(userDto.getEmail());
    }


    @Test
    public void shouldResetPasswordGivenNewPassword() throws Exception {

        UserDto userDto = new UserDto();
        userDto.setConfirmedPassword("newPassword");
        userDto.setPassword("newPassword");
        userDto.setEmail("user@quanta.com");

        String jwt = "jwt|_token";
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(jwt);

        ResponseEntity response = new ResponseEntity(tokenDto, HttpStatus.OK);

        given(this.userManagementFacade.resetPassword(userDto))
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

        verify(userManagementFacade).resetPassword(userDto);
    }


    @After
    public void tearDown() {

        userManagementFacade = null;
    }


}
