package com.quantal.exchange.users.services;

import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
import com.quantal.exchange.users.dto.ApiGatewayUserResponseDto;
import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.exchange.users.services.implementations.UserServiceImpl;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import com.quantal.javashared.util.CommonUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Created by dman on 10/04/2017.
 */

@RunWith(SpringRunner.class)
public class UserServiceTests {

    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @MockBean
    private MessageService messageService;

    @Mock
    private NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper;

    @Mock
    private ApiGatewayService apiGatewayService;

    @Mock
    private OrikaBeanMapper orikaBeanMapper;

    @Mock
    private PasswordService passwordService;

    @Mock
    private MDCAdapter mdcAdapter;

    private PasswordValidator passwordValidator;

    private String persistedModelFirstName =  "createdUserFirstName";
    private String persistedModelLastName = "createdUserLastName";
    private String persistedModelEmail = "createdUser@quant.com";
    private String persistedModelPassword = "createdUserPassword@1";
    private LocalDate dob = LocalDate.of(1990, 01, 01);
    private Long userId = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp(){

        userService = new UserServiceImpl(userRepository,
                messageService,
                orikaBeanMapper,
                nullSkippingOrikaBeanMapper,
                apiGatewayService,
                passwordService);

        passwordValidator = new PasswordValidator(Arrays.asList( new CharacterRule(EnglishCharacterData.Digit, 1)));
    }

    @Test
    public void shouldThrowNullPointerExceptionGivenUserModelOnCreateUser () {

        String errMsg = "data provided cannot be null";

        // Then
        //thrown.expect(NullPointerException.class);
        //thrown.expectMessage(errMsg);

        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);

        // When
        try {
            userService.createUser(null, mdcAdapter).get();
        } catch (Throwable ex)

        {
            // Then

            assertThat(ex instanceof NullPointerException).isTrue();
            verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);
        }

    }

    @Test
    public void shouldThrowAlreadyExistsExceptionGivenAlreadyExistentUserModelOnCreateUser () throws ExecutionException, InterruptedException {

        User userToSave = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        User result = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        String msgSvcMsg = "already exists";
        String partialErrMsg = String.format("user with to %s ", userToSave.getEmail().toLowerCase());
        String errMsg = String.format("%s%s", partialErrMsg, msgSvcMsg);
        ApiGatewayUserRequestDto apiGatewayUserRequestDto = UserTestUtil.createApiGatewayUserDto(//null,
                persistedModelEmail.trim().toLowerCase());

        // Given

        given(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{partialErrMsg})).willReturn(errMsg);
        given(userRepository.findOneByEmail(userToSave.getEmail().toLowerCase())).willReturn(result);
        try {
            // When
            CompletableFuture future = userService.createUser(userToSave, mdcAdapter);
            future.get();

        } catch (Throwable ex) {

            // Then

            assertThat(ex.getCause() instanceof AlreadyExistsException).isTrue();
            //assertThat(ex.getCause() instanceof java.util.concurrent.ExecutionException);
            assertThat(ex.getMessage().equalsIgnoreCase(errMsg));

            verify(messageService).getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{partialErrMsg});
            verify(userRepository).findOneByEmail(userToSave.getEmail());
        }

    }

    @Test
    public void shouldThrowPasswordValidationExcectionGivenUserModelWithInvalidExceptionOnCreateUser () throws ExecutionException, InterruptedException {

        User userToSave = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                "Password@",
                GenderEnum.M, dob);

        // Given
        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(false);
        given(passwordService.checkPasswordValidity(userToSave.getPassword())).willReturn(ruleResult);
        given(passwordService.getPasswordValidationCheckErrorMessages(ruleResult,"\n")).willReturn("");

        try {
            // When
            CompletableFuture future = userService.createUser(userToSave, mdcAdapter);

            future.get();
        } catch (Throwable ex) {

            // Then
            assertThat(ex.getCause() instanceof PasswordValidationException).isTrue();

            verify(passwordService).checkPasswordValidity(userToSave.getPassword());
            verify(passwordService).getPasswordValidationCheckErrorMessages(ruleResult,"\n");
        }

    }

    @Test
    public void shouldCreateNewUserGivenUserModel () throws ExecutionException, InterruptedException {

        String hashedPassword = "$2a$10$ZpK.XFLeMPjsvwvFKx/CeOL.lncdO4vSyHh/MI3xl0I/2uIs8wSu6";

        User userToSave = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                hashedPassword,
                GenderEnum.M, dob);

        User createdUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                hashedPassword,
                GenderEnum.M, dob);




        ApiGatewayUserRequestDto apiGatewayUserRequestDto = UserTestUtil.createApiGatewayUserDto(//null,
                persistedModelEmail.trim().toLowerCase());

        // Given

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(true);

        given(passwordService.hashPassword(userToSave.getPassword())).willReturn(hashedPassword);
        given(passwordService.checkPasswordValidity(userToSave.getPassword())).willReturn(ruleResult);
        given(userRepository.findOneByEmail(userToSave.getEmail())).willReturn(null);
        given(userRepository.save(eq(userToSave))).willReturn(createdUser);


        // **** RETURNING AN ANSWER ****
        given(apiGatewayService.addUer(apiGatewayUserRequestDto)).willAnswer(invocation -> CompletableFuture.completedFuture(new ApiGatewayUserResponseDto()));

        // When
        CompletableFuture completableFutureResult = userService.createUser(userToSave, mdcAdapter);

        // Get the result of the completable future
        User result = (User) completableFutureResult.get();

        // Then
        assertThat(java.util.Objects.equals(result, createdUser));

        verify(userRepository).save(eq(userToSave));
        verify(userRepository).findOneByEmail(userToSave.getEmail());
        verify(apiGatewayService).addUer(apiGatewayUserRequestDto);
        verify(passwordService).hashPassword(userToSave.getPassword());
        verify(passwordService).checkPasswordValidity(userToSave.getPassword());

    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullUserUpdateOnUpdateUser () {

        String errMsg = "Update object data provided cannot be null";
        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);

        // When
        try {
            userService.updateUser(null).get();

        } catch (Throwable ex){
            assertThat(ex instanceof NullPointerException).isTrue();
            assertThat(ex.getMessage()).isEqualToIgnoringCase(errMsg);
            verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);


        }


    }

    @Test
    public void shouldThrowNullPointerExceptionGivenNullUserIdOnUpdateUser () {

        String errMsg = "data provided cannot be null";


        // Given
        given(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED)).willReturn(errMsg);
        try {
            // When
            userService.updateUser(new User()).get();
        } catch (Throwable ex) {
            // Then
            Exception businessEx = CommonUtils.extractBusinessException(ex);
            assertThat(businessEx instanceof NullPointerException);
            assertThat(businessEx.getMessage().equalsIgnoreCase("UserId of update object " + errMsg));

            verify(messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED);

        }
    }

    @Test
    public void shouldThrowNotFoundExceptionGivenNonExistentUserIdOnUpdateUser () {

        String errMsg = "User not found";
        String userClassName = User.class.getSimpleName();

        User updateData = UserTestUtil.createUserModel(userId,
                "UpdatedUserFirstName",
                "UpdatedUserLastName",
                persistedModelEmail,
                null,
                GenderEnum.M, dob);

        // Given
        given(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{userClassName})).willReturn(errMsg);
        given(userRepository.findOne(0L)).willAnswer(invocationOnMock -> null);


        // When
        try {
            userService.updateUser(updateData).get();
        } catch (Throwable ex) {

            // Then
            Exception businessEx = CommonUtils.extractBusinessException(ex);
            assertThat(businessEx instanceof NotFoundException).isTrue();
            assertThat(businessEx.getMessage()).isEqualToIgnoringCase(errMsg);
        }
    }


    @Test
    public void shouldUpdateUserGivenValidUpdateDataOnUpdateUser () throws ExecutionException, InterruptedException {

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
                GenderEnum.M, dob);

        User updatedUser = UserTestUtil.createUserModel(userId,
                updatedUserFirstName,
                updatedUserLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        // Given
        given(userRepository.findOne(userId))
                .willAnswer(invocationOnMock -> persistedUser);
        given(userRepository.save(updatedUser))
                .willAnswer(invocationOnMock -> updatedUser);
        doNothing().when(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);

        // When
        User result = (User) userService.updateUser(updateData).get();
        // Then
        assertThat(java.util.Objects.equals(updatedUser, result));

        verify(userRepository).findOne(userId);
        verify(userRepository).save(updatedUser);
        verify(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);


    }

    @Test
    public void shouldThrowInvalidUserPasswordGivenInvalidPasswordOnUpdateUser () throws ExecutionException, InterruptedException {

        String updatePassword = "updatePassword";

       // thrown.expect(java.util.concurrent.ExecutionException.class);

        User updateData = UserTestUtil.createUserModel(userId,
                null,
                null,
                null,
                updatePassword,
                null,
                null);

        User persistedUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        // Given
        given(userRepository.findOne(userId)).willReturn(persistedUser);

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(false);
        given(passwordService.checkPasswordValidity(updateData.getPassword())).willReturn(ruleResult);
        given(passwordService.getPasswordValidationCheckErrorMessages(ruleResult, "\n")).willReturn("");

        doNothing().when(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);


       try {
            // When
            CompletableFuture future = userService.updateUser(updateData);
            future.get();
        } catch (Throwable ex){
           Exception businessEx = CommonUtils.extractBusinessException(ex);
            assertThat(businessEx instanceof PasswordValidationException).isTrue();
            verify(userRepository).findOne(userId);
          //  verify(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);
            verify(passwordService).checkPasswordValidity(updateData.getPassword());
            verify(passwordService).getPasswordValidationCheckErrorMessages(ruleResult, "\n");
        }

    }

    @Test
    public void shouldUpdateUserGiveValidPasswordOnUpdateUser () {

        String updatePassword = "updatePassword@1";
        String updatedPasswordHash = "$2a$10$ZpK.XFLeMPjsvwvFKx/CeOL.lncdO4vSyHh/MI3xl0I/2uIs8wSu6";


        User updateData = UserTestUtil.createUserModel(userId,
                null,
                null,
                null,
                updatePassword,
                null,
                null);

        User persistedUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        User updatedUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                updatedPasswordHash,
                GenderEnum.M, dob);

        // Given
        given(userRepository.findOne(userId)).willReturn(persistedUser);
        given(userRepository.save(updatedUser)).willReturn(updatedUser);

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(true);
        given(passwordService.checkPasswordValidity(updateData.getPassword())).willReturn(ruleResult);

        doNothing().when(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);

        // When
        userService.updateUser(updateData)
                .thenAccept(result -> {

                    // Then
                    assertThat(java.util.Objects.equals(updatedUser, result));

                    verify(userRepository).findOne(userId);
                    verify(userRepository).save(updatedUser);
                    verify(nullSkippingOrikaBeanMapper).map(updateData, persistedUser);
                    verify(passwordService).checkPasswordValidity(updateData.getPassword());

                });

    }

    @Test
    public void shouldThrowAlreadyExistsExceptionGivenUserUpdateDataWithAnExistingEmailNotBelongToTheUserToBeUpdated () throws ExecutionException, InterruptedException {

        String updateEmail = "user2@quant.com";


        User updateData = UserTestUtil.createUserModel(userId,
                null,
                null,
                updateEmail,
                null,
                null,
                null);

        User persistedUser = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        String msgSvcMsg = "already exists";
        String partialErrMsg = String.format("user with to %s ", updateData.getEmail());
        String errMsg = String.format("%s%s", partialErrMsg, msgSvcMsg);

        // Then
        User updateModel2 = UserTestUtil.createUserModel(2L,
                "userFName",
                "userLName",
                "user2@quant.com",
                null,
                null,
                null);

        List<User> users = Arrays.asList(persistedUser, updateModel2);
        // Given
        given(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS,  new String[]{partialErrMsg})).willReturn(errMsg);
        given(userRepository.findAllByEmailIgnoreCase(updateData.getEmail())).willAnswer(invocationOnMock -> users);
        given(userRepository.findOne(userId)).willAnswer(invocationOnMock -> persistedUser);


        // When
        try {
            userService.updateUser(updateData).get();
        } catch (Throwable ex) {
            Throwable businessEx = CommonUtils.extractBusinessException(ex);
            assertThat(businessEx instanceof AlreadyExistsException).isTrue();
            verify(messageService).getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{partialErrMsg});
            verify(userRepository).findAllByEmailIgnoreCase(updateData.getEmail());
            verify(userRepository).findOne(userId);
        }


    }

    @Test
    public void shouldReturnOneUserGivenUserIdOnFindOne () throws ExecutionException, InterruptedException {

        User user = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        // Given

        given(userRepository.findOne(userId)).willReturn(user);

        // When

        User result = userService.findOne(userId).get();

        // Then

        assertThat(java.util.Objects.equals(user, result));

        verify(userRepository).findOne(userId);
    }

    @Test
    public void shouldReturnOneUserGivenUserEmailOnFindByEmail () throws ExecutionException, InterruptedException {

        User user = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        // Given

        given(userRepository.findOneByEmail(persistedModelEmail)).willReturn(user);

        // When

        User result = userService.findOneByEmail(persistedModelEmail, mdcAdapter).get();

        // Then

        assertThat(java.util.Objects.equals(user, result));

        verify(userRepository).findOneByEmail(persistedModelEmail);
    }

    @Test
    public void shouldDeleteUserGivenUserIdOnDelete () throws ExecutionException, InterruptedException {
        // Given

       doNothing().when(userRepository).delete(userId);

       doAnswer(invocationOnMock -> CompletableFuture.completedFuture(null))
               .when(userRepository).delete(userId);

        // When

        userService.deleteById(userId).get();

        // Then

        verify(userRepository).delete(userId);
    }

    @Test
    public void shouldThrowNotFoundExceptionGivenNonExistentUserIdOnDeletUserById () {


        doAnswer(invocationOnMock -> {
            throw new EmptyResultDataAccessException(0);
        }
        ).when(userRepository).delete(userId);

        // When
        try {
            userService.deleteById(userId).get();
        } catch (Throwable ex) {
            assertThat(ex.getCause() instanceof NotFoundException);
            verify(userRepository).delete(userId);
        }
    }

    @Test
    public void shouldReturnTheSavedUserGivenUserModelToSaveOnSaveOrUpdate () throws ExecutionException, InterruptedException {

        User expected = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        User saveData = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        // Given

        given(userRepository.save(saveData)).willReturn(expected);

        // When

        User result = userService.saveOrUpdate(saveData).get();

        // Then

        assertThat(java.util.Objects.equals(expected, result));

        verify(userRepository).save(saveData);
    }



    @After
    public void tearDown() {

    }

}
