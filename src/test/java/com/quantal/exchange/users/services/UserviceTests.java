package com.quantal.exchange.users.services;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.objectmapper.OrikaBeanMapper;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.exchange.users.services.implementations.UserServiceImpl;
import com.quantal.exchange.users.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * Created by dman on 10/04/2017.
 */

@RunWith(SpringRunner.class)
public class UserviceTests {

    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MessageService messageService;

    @MockBean
    private OrikaBeanMapper orikaBeanMapper;

    private String persistedModelFirstName =  "createdUserFirstName";
    private String persistedModelLastName = "createdUserLastName";
    private String persistedModelEmail = "createdUser@quant.com";
    private String persistedModelPassword = "createdUserPassword";
    private LocalDate dob = LocalDate.of(1990, 01, 01);
    private Long userId = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp(){

        userService = new UserServiceImpl(userRepository, messageService, orikaBeanMapper);
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenUserModelOnCreateUser () {

        String errMsg = "data provided cannot be null";

        // Then
        thrown.expect(NullPointerException.class);
        thrown.expectMessage(errMsg);

        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);

        // When
        userService.createUser(null);
        verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);

    }

    @Test
    public void shouldThrowAlreadyExistsExceptionGivenAlreadyExistentUserModelOnCreateUser () {

        User userToSave = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        User result = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        String msgSvcMsg = "already exists";
        String partialErrMsg = String.format("user with email %s ", userToSave.getEmail());
        String errMsg = String.format("%s%s", partialErrMsg, msgSvcMsg);

        // Then
        thrown.expect(AlreadyExistsException.class);
        thrown.expectMessage(errMsg);

        // Given
        given(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS,  new String[]{partialErrMsg})).willReturn(errMsg);
        given(userRepository.findOneByEmail(userToSave.getEmail())).willReturn(result);


        // When
        userService.createUser(userToSave);

        verify(messageService).getMessage(MessageCodes.ENTITY_ALREADY_EXISTS,  new String[]{partialErrMsg});
        verify(userRepository).findOneByEmail(userToSave.getEmail());
    }

    @Test
    public void shouldCreateNewUserGivenUserModel () {

        User userToSave = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        User createdUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given
        given(userRepository.findOneByEmail(userToSave.getEmail())).willReturn(null);
        given(userRepository.save(userToSave)).willReturn(createdUser);

        // When
        User result = userService.createUser(userToSave);

        // Then
        assertThat(java.util.Objects.equals(result, createdUser));

        verify(userRepository).save(userToSave);
        verify(userRepository).findOneByEmail(userToSave.getEmail());
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullUserUpdateOnUpdateUser () {

        String errMsg = "data provided cannot be null";

        // Then
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Update object " + errMsg);

        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);

        // When
        userService.updateUser(null);
        verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullUserIdOnUpdateUser () {

        String errMsg = "data provided cannot be null";

        // Then
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("UserId of update object "+errMsg);

        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);

        // When
        userService.updateUser(new User());
        verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);

    }

    @Test
    public void shouldThrowNotFoundExceptionGivenNonExistentUserIdOnUpdateUser () {

        String errMsg = "User not found";
        String userClassName = User.class.getSimpleName();

        User updateData = UserTestUtil.createUserModel(userId,
                "UpdatedUserFirstName",
                "UpdatedUserLastName",
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given
        given(messageService.getMessage(MessageCodes.NOT_FOUND, new String[] {userClassName})).willReturn(errMsg);
        given(userRepository.findOne(0L)).willReturn(null);

        // Then
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(errMsg);

        // When
        userService.updateUser(updateData);
    }


    @Test
    public void shouldUpdateUserGivenValidUpdateOnUpdateUser () {

        String updatedUserFirstName = "UpdatedUserFirstName";
        String updatedUserLastName = "UpdatedUserLastName";


        User updateData = UserTestUtil.createUserModel(userId,
                updatedUserFirstName,
                updatedUserLastName,
                null,
                null,
                null,
                null);

        User persistedUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        User updatedUser = UserTestUtil.createUserModel(userId,
                updatedUserFirstName,
                updatedUserLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given
        given(userRepository.findOne(userId)).willReturn(persistedUser);
        given(userRepository.save(updatedUser)).willReturn(updatedUser);
        doNothing().when(orikaBeanMapper).map(updateData, persistedUser);

        // When
        User result = userService.updateUser(updateData);

        // Then
        assertThat(java.util.Objects.equals(updatedUser, result));

        verify(userRepository).findOne(userId);
        verify(userRepository).save(updatedUser);
        verify(orikaBeanMapper).map(updateData, persistedUser);


    }

    @Test
    public void shouldReturnOneUserGivenUserIdOnFindOne (){

        User user = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given

        given(userRepository.findOne(userId)).willReturn(user);

        // When

        User result = userService.findOne(userId);

        // Then

        assertThat(java.util.Objects.equals(user, result));

        verify(userRepository).findOne(userId);
    }

    @Test
    public void shouldReturnOneUserGivenUserEmailOnFindByEmail (){

        User user = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given

        given(userRepository.findOneByEmail(persistedModelEmail)).willReturn(user);

        // When

        User result = userService.findOneByEmail(persistedModelEmail);

        // Then

        assertThat(java.util.Objects.equals(user, result));

        verify(userRepository).findOneByEmail(persistedModelEmail);
    }

    @Test
    public void shouldDeleteUserGivenUserIdOnDelete (){
        // Given

       doNothing().when(userRepository).delete(userId);

        // When

        userService.delete(userId);

        // Then

        verify(userRepository).delete(userId);
    }

    @Test
    public void shouldReturnTheSavedUserGivenUserModelToSaveOnSaveOrUpdate (){

        User expected = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        User saveData = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        // Given

        given(userRepository.save(saveData)).willReturn(expected);

        // When

        User result = userService.saveOrUpdate(saveData);

        // Then

        assertThat(java.util.Objects.equals(expected, result));

        verify(userRepository).save(saveData);
    }

    @After
    public void tearDown() {

    }

}
