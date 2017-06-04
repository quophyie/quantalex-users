package com.quantal.exchange.users.facades;


import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.interfaces.PasswordService;
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
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.in;
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

    @MockBean
    private PasswordService passwordService;


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

        ApiJwtUserCredentialResponseDto jwtUserCredentialResponseDto = new ApiJwtUserCredentialResponseDto();
        jwtUserCredentialResponseDto.setKey("TestKey");

        given(this.userService
                .createUser(eq(userModelFromDto)))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(createdModel));


        given(this.userService
                .requestApiGatewayUserCredentials(persistedModelEmail))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(jwtUserCredentialResponseDto));

        given(this.userService
                .createJwt(jwtUserCredentialResponseDto.getKey()))
                .willReturn("token");

        given(this.messageService.getMessage(MessageCodes.ENTITY_CREATED, replacements))
                .willReturn(successMsg);

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
        assertThat(result.getToken()).isEqualTo("token");

        verify(userService, times(1)).createUser(userModelFromDto);
        verify(this.messageService).getMessage(MessageCodes.ENTITY_CREATED, replacements);
        verify(this.userService).createJwt(jwtUserCredentialResponseDto.getKey());
        verify(this.userService).requestApiGatewayUserCredentials(persistedModelEmail);
    }

    @Test
    public void shouldReturn409ConflictGivenAUserThatAlreadyExists() throws Exception {

        String persistedModelFirstName = "createdUserFirstName";
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

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto).get();
        String message = TestUtil.getResponseDtoMessage(responseEntity);

        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(userService, times(1)).createUser(eq(userModelFromDto));
     }

    @Test
    public void shouldReturn400BadRequest() throws Exception {

        String persistedModelFirstName = "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = "data provided cannot be null";
        String[] replacements = new String[]{User.class.getSimpleName()};

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

        given(this.messageService
                .getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements))
                .willReturn(errMsg);

        given(this.userService
                .createUser(eq(userModelFromDto)))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new NullPointerException(errMsg));
                    return future;

                });

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto).get();

        String message = TestUtil.getResponseDtoMessage(responseEntity);

        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(this.messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements);
        verify(userService, times(1)).createUser(eq(userModelFromDto));
    }

    @Test
    public void shouldThrowAlreadyExistsExceptionGivenUserUpdateDataWithAnExistingEmailNotBelongingToTheUserToBeUpdated () throws ExecutionException, InterruptedException {

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
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new AlreadyExistsException(errMsg));
                    return future;
                });

        // When
        ResponseEntity responseEntity = userManagementFacade.updateUser(userId, updateData).get();
        // Then
        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CONFLICT);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        UserDto result = TestUtil.getResponseDtoData(responseEntity);
        assertThat(result).isNull();
        verify(userService).updateUser(eq(updateModel));
    }

    @Test
    public void shouldUpdateUserWithPartialData() throws Exception {

        String persistedModelFirstName = "persistedDtoFirstName";
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
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(updateModel));

        CompletableFuture future = userManagementFacade.updateUser(id, updateDto);
        ResponseEntity responseEntity = (ResponseEntity) future.get();

        HttpStatus httpStatusCode = responseEntity.getStatusCode();
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
        String persistedModelFirstName = "persistedUserFirstName";
        String persistedModelLastName = "persistedUserLastName";
        String persistedModelEmail = "persistedUser@quant.com";
        String persistedModelPassword = "persistedUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String updatedUserFirstName = "updatedUserFirstName";
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
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(persistedModel));

        given(this.userService
                .updateUser(eq(updateModel)))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(updateModel));

        ResponseEntity responseEntity = userManagementFacade.updateUser(id, updateDto).get();
        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);

        UserDto result = ((ResponseDto<UserDto>) responseEntity.getBody()).getData();

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
    public void shouldReturnUserGivenUserId() throws ExecutionException, InterruptedException {

        User persistedModel = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M,
                persistedModelDob);


        given(this.userService
                .findOne(userId))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(persistedModel));

        given(this.messageService
                .getMessage(MessageCodes.SUCCESS))
                .willReturn("OK");


        ResponseEntity<?> responseEntity = userManagementFacade.findUserById(userId).get();

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
                .findOne(2L))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(null));

        ResponseEntity<?> responseEntity = userManagementFacade.findUserById(2L).get();

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

        doAnswer(invocationOnMock -> CompletableFuture.completedFuture(null)).when(userService)
                .deleteById(userId);

        ResponseEntity responseEntity = (ResponseEntity) userManagementFacade.deleteByUserId(userId).get();

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

        doAnswer(invocationOnMock -> {
            CompletableFuture<Void> future = new CompletableFuture();
            future.completeExceptionally(new NotFoundException(""));
            return future;
        })
                .when(this.userService)
                .deleteById(userToDelId);

        ResponseEntity responseEntity = (ResponseEntity) userManagementFacade.deleteByUserId(userToDelId).get();

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
        ResponseEntity responseEntity = userManagementFacade.updateUser(2L, userDto).get();
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
                .willAnswer( invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new NotFoundException(errMsg));
                    return future;
                });

        given(messageService.getMessage(MessageCodes.NOT_FOUND,replacements )).willReturn(errMsg);
        UserDto userDto = UserTestUtil.createApiGatewayUserDto(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);

        ResponseEntity responseEntity = userManagementFacade.updateUser(id, userDto).get();

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.NOT_FOUND);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat("user not found").isEqualToIgnoringCase(message);
        verify(userService, times(1)).updateUser(userUpdateData);


    }

    @Test
    public void should400BadRequestGivenInvalidPassword() throws Exception {

        String persistedModelPassword = "pass";

        Long id = 100L;

        String errMsg = "Password must be at least 6 characters in length.";
        String[] replacements = new String[]{User.class.getSimpleName()};

        User userUpdateData = UserTestUtil.createUserModel(id,
                null,
                null,
                null,
                persistedModelPassword,
                null, null);

        given(this.userService
                .updateUser(userUpdateData))
                .willAnswer( invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new PasswordValidationException(errMsg));
                    return future;
                });

        UserDto userDto = UserTestUtil.createApiGatewayUserDto(id,
                null,
                null,
                null,
                persistedModelPassword,
                null, null);


        ResponseEntity responseEntity = userManagementFacade.updateUser(id, userDto).get();

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(errMsg).isEqualToIgnoringCase(message);
        verify(userService, times(1)).updateUser(userUpdateData);


    }


}
