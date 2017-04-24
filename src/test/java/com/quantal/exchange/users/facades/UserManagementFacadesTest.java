package com.quantal.exchange.users.facades;


import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.shared.dto.ResponseDto;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.shared.util.TestUtil;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by dman on 25/03/2017.
 */
@RunWith(SpringRunner.class)
//@WebMvcTest(UserManagementFacade.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserManagementFacadesTest {


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String persistedModelFirstName =  "createdUserFirstName";
    private String persistedModelLastName = "createdUserLastName";
    private String persistedModelEmail = "createdUser@quant.com";
    private String persistedModelPassword = "createdUserPassword";
    private LocalDate persistedModelDob = LocalDate.of(1990, 01, 01);
    private Gender persistedModelGender = Gender.M;
    private Long userId = 1L;

    @MockBean
    private UserService userService;

    @MockBean
    private MessageService messageService;


    @Autowired
    @InjectMocks
    private UserManagementFacade userManagementFacade;

    @Before
    public void setUp(){
     //userManagementFacade = new UserManagementFacade(userService, giphyApiService);
    }

    @Test
    public void shouldCreateNewUser() throws Exception {

        persistedModelFirstName =  "createdUserFirstName";
        persistedModelLastName = "createdUserLastName";
        persistedModelEmail = "createdUser@quant.com";
        persistedModelPassword = "createdUserPassword";
        persistedModelDob = LocalDate.of(1990, 01, 01);
        userId = 1L;

        String successMsg = "User created successfully";
        String[] replacements = new String[]{User.class.getSimpleName()};

        User createdModel = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, persistedModelDob);

        User userModelFromDto = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, persistedModelDob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, persistedModelDob);

        CompletableFuture userServiceCompletableFuture = new CompletableFuture();
        userServiceCompletableFuture.complete(createdModel);
        given(this.userService
                .createUser(eq(userModelFromDto)))
                .willReturn(userServiceCompletableFuture);

        given(this.messageService.getMessage(MessageCodes.ENTITY_CREATED, replacements))
                .willReturn(successMsg);

        CompletableFuture completableFuture = new CompletableFuture();
        ResponseEntity<?> completableFutureResponseEntity = new ResponseEntity<Object>(createdModel, HttpStatus.CREATED);
        completableFuture.complete(completableFutureResponseEntity);

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto).get();
        UserDto result = ((ResponseDto<UserDto>)responseEntity.getBody()).getData();
        String message = ((ResponseDto<UserDto>)responseEntity.getBody()).getMessage();

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CREATED);
        assertThat(successMsg).isEqualToIgnoringCase(message);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo(persistedModelFirstName);
        assertThat(result.getLastName()).isEqualTo(persistedModelLastName);
        assertThat(result.getEmail()).isEqualTo(persistedModelEmail);
        assertThat(result.getPassword()).isEqualTo(persistedModelPassword);
        assertThat(result.getDob()).isEqualTo(persistedModelDob);
        assertThat(result.getGender()).isEqualTo(Gender.M);

        verify(userService, times(1)).createUser(userModelFromDto);
        verify(this.messageService).getMessage(MessageCodes.ENTITY_CREATED, replacements);
    }

    @Test
    public void shouldReturn409ConflictGivenAUserThatAlreadyExists() throws Exception {

        String persistedModelFirstName =  "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = String.format("user with email %s already exists", persistedModelEmail);

        User userModelFromDto = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        given(this.userService
                .createUser(eq(userModelFromDto)))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new AlreadyExistsException(errMsg));
                    return future;
                });

        //ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto).get();
        userManagementFacade.save(createUserDto).thenAccept(responseEntity -> {
            String message = TestUtil.getResponseDtoMessage(responseEntity);

            HttpStatus httpStatusCode  = responseEntity.getStatusCode();
            assertThat(httpStatusCode).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errMsg).isEqualToIgnoringCase(message);

            verify(userService, times(1)).createUser(eq(userModelFromDto));
        });

    }

    @Test
    public void shouldReturn400BadRequest() throws Exception {

        String persistedModelFirstName =  "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = "data provided cannot be null";
        String[] replacements = new String[]{User.class.getSimpleName()};

        User userModelFromDto =  UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        given(this.messageService
                .getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements))
                .willReturn(errMsg);

        given(this.userService
                .createUser(eq(userModelFromDto)))
                .willAnswer(invocationOnMock ->  {
                    CompletableFuture future  = new CompletableFuture();
                    future.completeExceptionally(new NullPointerException(errMsg));
                    return future;

                });

       // ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto).get();

        userManagementFacade.save(createUserDto).thenAccept( responseEntity -> {
            String message = TestUtil.getResponseDtoMessage(responseEntity);

            HttpStatus httpStatusCode  = responseEntity.getStatusCode();
            assertThat(httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errMsg).isEqualToIgnoringCase(message);

            verify(this.messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements);
            verify(userService, times(1)).createUser(eq(userModelFromDto));
        });

    }

    @Test
    public void shouldThrowAlreadyExistsExceptionGivenUserUpdateDataWithAnExistingEmailNotBelongingToTheUserToBeUpdated () {

        String updatedUserFirstName = "UpdatedUserFirstName";
        String updatedUserLastName = "UpdatedUserLastName";


        UserDto updateData = UserTestUtil.createApiGatewayUserDto(userId,
                null,
                null,
                persistedModelEmail,
                null,
                null,
                null);

        User updateModel = UserTestUtil.createUserModel(userId,
                null,
                null,
                persistedModelEmail,
                null,
                null,
                null);

        String msgSvcMsg = "already exists";
        String partialErrMsg = String.format("user with email %s ", updateData.getEmail());
        String errMsg = String.format("%s%s", partialErrMsg, msgSvcMsg);

        // Given
        given(this.userService
                .updateUser(eq(updateModel)))
                .willThrow(new AlreadyExistsException(errMsg));

        // When
        ResponseEntity<?> responseEntity =   userManagementFacade.updateUser(userId, updateData);;

        // Then
        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CONFLICT);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        UserDto result = TestUtil.getResponseDtoData(responseEntity);
        assertThat(result).isNull();
        verify(userService).updateUser(eq(updateModel));


    }

    @Test
    public void shouldUpdateUserWithPartialData() throws Exception {

        String persistedModelFirstName =  "persistedDtoFirstName";
        String persistedModelLastName = "persistedDtoLastName";
        String persistedModelEmail = "persistedModel@quant.com";
        String persistedModelPassword = "persistedDtoPassword";
        String updateDtoFirstName = "updatedFirstName";
        String updateDtoLastName = "updatedLastName";
        String updateDtoEmail = "updateModel@quant.com";
        Long id = 1L;

        User persistedModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);

        User updateModel = UserTestUtil.createUserModel(id,
                updateDtoFirstName,
                updateDtoLastName,
                updateDtoEmail,
                persistedModelPassword,
                Gender.M, null);

        UserDto updateDto = UserTestUtil.createApiGatewayUserDto(id,
                updateDtoFirstName,
                updateDtoLastName,
                updateDtoEmail,
                null,
                null,
                null);

        String[] replacements = new String[]{User.class.getSimpleName()};
        String successMsg = "User updated successfully";

        given(this.messageService
                .getMessage(MessageCodes.ENTITY_UPDATED, replacements))
                .willReturn(successMsg);

        given(this.userService
                .updateUser(eq(updateModel)))
                .willReturn(updateModel);

        ResponseEntity<?> responseEntity = userManagementFacade.updateUser(id, updateDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(successMsg).isEqualToIgnoringCase(message);

        UserDto result = TestUtil.getResponseDtoData(responseEntity);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(updateDtoFirstName);
        assertThat(result.getLastName()).isEqualTo(updateDtoLastName);
        assertThat(result.getEmail()).isEqualTo(updateDtoEmail);
        assertThat(result.getPassword()).isEqualTo(persistedModelPassword);
        assertThat(result.getGender()).isEqualTo(Gender.M);
        verify(userService, times(1)).updateUser(eq(updateModel));
        verify(messageService).getMessage(MessageCodes.ENTITY_UPDATED, replacements);

    }

    @Test
    public void shouldUpdateUserWithFullData() throws Exception {

        Long id = 1L;
        String persistedModelFirstName =  "persistedUserFirstName";
        String persistedModelLastName = "persistedUserLastName";
        String persistedModelEmail = "persistedUser@quant.com";
        String persistedModelPassword = "persistedUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String updatedUserFirstName =  "updatedUserFirstName";
        String updatedUserLastName = "updatedUserLastName";
        String updatedEmail = "updatedUser@quant.com";
        String updatedUserPassword = "updatedUserPassword";
        LocalDate updatedDob = LocalDate.of(1980, 11, 01);



        User persistedModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M,
                dob);

        User updateModel = UserTestUtil.createUserModel(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                Gender.F,
                updatedDob);

        UserDto updateDto = UserTestUtil.createApiGatewayUserDto(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                Gender.F,
                updatedDob);


        given(this.userService
                .findOne(id))
                .willReturn(persistedModel);

        given(this.userService
                .updateUser(eq(updateModel)))
                .willReturn(updateModel);

        ResponseEntity<?> responseEntity = userManagementFacade.updateUser(id, updateDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);

        UserDto result = ((ResponseDto<UserDto>)responseEntity.getBody()).getData();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(updatedUserFirstName);
        assertThat(result.getLastName()).isEqualTo(updatedUserLastName);
        assertThat(result.getEmail()).isEqualTo(updatedEmail);
        assertThat(result.getPassword()).isEqualTo(updatedUserPassword);
        assertThat(result.getGender()).isEqualTo(Gender.F);
        verify(userService, times(1)).updateUser(eq(updateModel));
    }

    @Test
    public void shouldReturnUserGivenUserId() {

        User persistedModel = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M,
                persistedModelDob);


        given(this.userService
                .findOne(userId))
                .willReturn(persistedModel);

        given(this.messageService
                .getMessage(MessageCodes.SUCCESS))
                .willReturn("OK");


        ResponseEntity<?> responseEntity = userManagementFacade.findUserById(userId);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);

        UserDto result = TestUtil.getResponseDtoData(responseEntity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo(persistedModelFirstName);
        assertThat(result.getLastName()).isEqualTo(persistedModelLastName);
        assertThat(result.getEmail()).isEqualTo(persistedModelEmail);
        assertThat(result.getDob()).isEqualTo(persistedModelDob);
        assertThat(result.getGender()).isEqualTo(persistedModelGender);
        verify(userService, times(1)).findOne(userId);
    }

    @Test
    public void should404NotFoundGiveninvalidUserId() throws Exception {
        String errMsg = "User not found";

        given(messageService.getMessage(MessageCodes.NOT_FOUND, new String[] {User.class.getSimpleName()})).willReturn(errMsg);

        given(this.userService
                .findOne(userId))
                .willReturn(null);

        ResponseEntity<?> responseEntity = userManagementFacade.findUserById(2L);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.NOT_FOUND);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(userService).findOne(2L);
        verify(messageService).getMessage(MessageCodes.NOT_FOUND, new String[] {User.class.getSimpleName()});

    }

    @Test
    public void shouldDeleteUserGivenUserId() throws Exception {
        String successMsg = "OK";
        User persistedModel = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M,
                persistedModelDob);

        given(messageService.getMessage(MessageCodes.SUCCESS)).willReturn(successMsg);

        doNothing().when(userService)
                .deleteById(userId);

        ResponseEntity<?> responseEntity = userManagementFacade.deleteByUserId(userId);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(successMsg).isEqualToIgnoringCase(message);

        verify(userService).deleteById(userId);
        verify(messageService).getMessage(MessageCodes.SUCCESS);

    }


    @Test
    public void should404NotFoundGiveninvalidUserIdOnDelete() throws Exception {
        String errMsg = "User not found";

        Long userToDelId = 2L;
        given(messageService.getMessage(MessageCodes.NOT_FOUND, new String[] {User.class.getSimpleName()})).willReturn(errMsg);

        doThrow(NotFoundException.class)
                .when(this.userService)
                .deleteById(userToDelId);

        ResponseEntity<?> responseEntity = userManagementFacade.deleteByUserId(userToDelId);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.NOT_FOUND);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(userService).deleteById(userToDelId);
        verify(messageService).getMessage(MessageCodes.NOT_FOUND, new String[] {User.class.getSimpleName()});

    }

    @Test
    public void should400BadRequestGivenNullUserDtoOnUserUpdate() throws Exception {

        String errMsg = "Data provided cannot be null";
        UserDto userDto = null;
        String[] replacements = new String[]{User.class.getSimpleName()};
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED,replacements )).willReturn(errMsg);
        ResponseEntity<?> responseEntity = userManagementFacade.updateUser(2L, userDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED,replacements );

    }

    @Test
    public void should404NotFoundIfSuppliedUserCannotBeFound() throws Exception {

       String persistedModelFirstName = "persistedDtoFirstName";
        String persistedModelLastName = "persistedDtoLastName";
        String persistedModelEmail = "persistedModel@quant.com";
        String persistedModelPassword = "persistedDtoPassword";

        Long id = 100L;

        String errMsg = "user not found";
        String[] replacements = new String[]{User.class.getSimpleName()};

        User userUpdateData = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);

        given(this.userService
                .updateUser(userUpdateData))
                .willThrow(new NotFoundException(errMsg));

        given(messageService.getMessage(MessageCodes.NOT_FOUND,replacements )).willReturn(errMsg);
        UserDto userDto = UserTestUtil.createApiGatewayUserDto(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);

        ResponseEntity<?> responseEntity = userManagementFacade.updateUser(id, userDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.NOT_FOUND);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat("user not found").isEqualToIgnoringCase(message);
        verify(userService, times(1)).updateUser(userUpdateData);

    }




}
