package com.quantal.exchange.users.facades;


import com.quantal.exchange.users.constants.EmailTemplates;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.AuthResponseDto;
import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.dto.EmailResponseDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.EmailType;
import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.enums.TokenType;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.InvalidDataException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.api.EmailApiService;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.dto.ResponseDto;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
import com.quantal.javashared.util.TestUtil;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.passay.RuleResult;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.quantal.exchange.users.constants.TestConstants.EVENT;
import static com.quantal.exchange.users.constants.TestConstants.TRACE_ID;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by dman on 25/03/2017.
 */
@RunWith(SpringRunner.class)
//@WebMvcTest(UserManagementFacade.class)
@SpringBootTest
@AutoConfigureMockMvc
/*@TestPropertySource(properties = {
        "DB_HOST=localhost",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialec",
        "flyway.url= jdbc:h2:mem:quantal_exchange_users;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "flyway.user= sa",
        "flyway.password=",
        "flyway.locations=classpath:db/migration",
        "flyway.baseline-on-migrate=true",
        "DB_PORT=5432",
        "spring.datasource.url=jdbc:h2:mem:quantal_exchange_users;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "API_GATEWAY_ENDPOINT=http://localhost"

})*/
public class UserManagementFacadesTest {


    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private String persistedModelFirstName =  "createdUserFirstName";
    private String persistedModelLastName = "createdUserLastName";
    private String persistedModelEmail = "createdUser@quant.com";
    private String persistedModelPassword = "createdUserPassword";
    private String persistedModelConfirmedPassword = "createdUserPassword";
    private LocalDate persistedModelDob = LocalDate.of(1990, 01, 01);
    private GenderEnum persistedModelGenderEnum = GenderEnum.M;
    private Long userId = 1L;

    @MockBean
    private UserService userService;

    @MockBean
    private GiphyApiService giphyApiService;

    @MockBean
    private MessageService messageService;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private AuthorizationApiService authorizationApiService;

    @Autowired
    @Qualifier("orikaBeanMapper")
    private OrikaBeanMapper orikaBeanMapper;

    @Autowired
    @Qualifier("nullSkippingOrikaBeanMapper")
    private NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper;

    @MockBean
    private EmailApiService emailApiService;


    @InjectMocks
    private UserManagementFacade userManagementFacade;


    @Mock
    private MDCAdapter mdcAdapter;


    @Before
    public void setUp(){
      //  nullSkippingOrikaBeanMapper = new NullSkippingOrikaBeanMapper();
      //  orikaBeanMapper = new OrikaBeanMapper();
       userManagementFacade = new UserManagementFacade(userService,
               giphyApiService,
               messageService,
               orikaBeanMapper,
               nullSkippingOrikaBeanMapper,
               authorizationApiService,
               emailApiService,
               passwordService);
        environmentVariables.set("DB_HOST", "localhost");

        mdcAdapter.put(CommonConstants.TRACE_ID_MDC_KEY, TRACE_ID);
        mdcAdapter.put(CommonConstants.EVENT_KEY, EVENT);

        QuantalLogger quantalLogger = QuantalLoggerFactory.getLogger(UserManagementFacade.class,  LoggerConfig.builder().commonLogFields(new CommonLogFields()).build());
        quantalLogger = (QuantalLogger) quantalLogger.with(CommonConstants.TRACE_ID_MDC_KEY, TRACE_ID).with(CommonConstants.EVENT_KEY, "EVENT");
        ReflectionTestUtils.setField(userManagementFacade, "logger", quantalLogger);

        given(mdcAdapter.get(CommonConstants.TRACE_ID_MDC_KEY)).willReturn(TRACE_ID);
        given(mdcAdapter.get(CommonConstants.EVENT_KEY)).willReturn(EVENT);
    }

    @After
    public void tearDown(){
        reset(mdcAdapter);
        reset(messageService);
        reset(authorizationApiService);
        reset(passwordService);
        reset(authorizationApiService);
        reset(emailApiService);
        reset(giphyApiService);
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
                GenderEnum.M, persistedModelDob);

        User userModelFromDto = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, persistedModelDob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, persistedModelDob);

        ApiJwtUserCredentialResponseDto jwtUserCredentialResponseDto = new ApiJwtUserCredentialResponseDto();
        jwtUserCredentialResponseDto.setKey("TestKey");

        String jwt = "jwt_token";

        AuthRequestDto authRequestDto = new AuthRequestDto();
        //authRequestDto.setTokenType(TokenType.PasswordReset);
        authRequestDto.setEmail(persistedModelEmail);

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(jwt);

        EmailRequestDto emailRequestDto = EmailRequestDto.builder()
                .to(userModelFromDto.getEmail()).build();

        EmailResponseDto emailResponseDto = EmailResponseDto.builder().code(200).build();


        given(this.userService
                .createUser(userModelFromDto, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(createdModel));


        given(this.userService
                .requestApiGatewayUserCredentials(persistedModelEmail))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(jwtUserCredentialResponseDto));

        given(this.userService
                .createJwt(jwtUserCredentialResponseDto.getKey()))
                .willReturn(jwt);

        given(this.messageService.getMessage(MessageCodes.ENTITY_CREATED, replacements))
                .willReturn(successMsg);

        given(authorizationApiService.requestUserCredentials(authRequestDto, mdcAdapter.get(CommonConstants.EVENT_KEY), mdcAdapter.get(CommonConstants.TRACE_ID_MDC_KEY)))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture( null));

        given(authorizationApiService.requestToken(authRequestDto, EVENT, TRACE_ID))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(tokenDto));

        given(emailApiService.sendEmailByTemplate(EmailTemplates.NEW_USER_TEMPLATE, emailRequestDto, EVENT, TRACE_ID))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(emailResponseDto));

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto, mdcAdapter).get();
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
        assertThat(result.getGender()).isEqualTo(GenderEnum.M);
        assertThat(result.getToken()).isEqualTo(jwt);

        verify(userService, times(1)).createUser(userModelFromDto, mdcAdapter);
        verify(this.messageService).getMessage(MessageCodes.ENTITY_CREATED, replacements);
        verify(this.authorizationApiService).requestUserCredentials(authRequestDto, EVENT, TRACE_ID);
        verify(this.authorizationApiService).requestToken(authRequestDto, EVENT, TRACE_ID);
        verify(emailApiService).sendEmailByTemplate(EmailTemplates.NEW_USER_TEMPLATE, emailRequestDto, EVENT, TRACE_ID);
//          verify(this.userService).requestApiGatewayUserCredentials(persistedModelEmail);
    }

    @Test
    @Ignore
    public void shouldReturn409ConflictGivenAUserThatAlreadyExists() throws Exception {

        String persistedModelFirstName = "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String errMsg = String.format("user with to %s already exists", persistedModelEmail);

        User userModelFromDto = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        given(this.userService
                .createUser(eq(userModelFromDto), mdcAdapter))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new AlreadyExistsException(errMsg));
                    return future;
                });

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto, mdcAdapter).get();
        String message = TestUtil.getResponseDtoMessage(responseEntity);

        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(userService, times(1)).createUser(eq(userModelFromDto), mdcAdapter);
     }

    @Test
    @Ignore
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
                GenderEnum.M, dob);


        UserDto createUserDto = UserTestUtil.createApiGatewayUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, dob);

        given(this.messageService
                .getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements))
                .willReturn(errMsg);

        given(this.userService
                .createUser(eq(userModelFromDto), mdcAdapter))
                .willAnswer(invocationOnMock -> {
                    CompletableFuture future = new CompletableFuture();
                    future.completeExceptionally(new NullPointerException(errMsg));
                    return future;

                });

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto, mdcAdapter).get();

        String message = TestUtil.getResponseDtoMessage(responseEntity);

        HttpStatus httpStatusCode = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errMsg).isEqualToIgnoringCase(message);

        verify(this.messageService).getMessage(MessageCodes.NULL_DATA_PROVIDED, replacements);
        verify(userService, times(1)).createUser(eq(userModelFromDto), mdcAdapter);
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
                GenderEnum.F,
                null);

        String msgSvcMsg = "already exists";
        String partialErrMsg = String.format("user with to %s ", updateData.getEmail());
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
                GenderEnum.M, null);

        User updateModel = UserTestUtil.createUserModel(id,
                updateDtoFirstName,
                updateDtoLastName,
                updateDtoEmail,
                persistedModelPassword,
                GenderEnum.M, null);

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
        assertThat(result.getGender()).isEqualTo(GenderEnum.M);
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
                GenderEnum.M,
                dob);

        User updateModel = UserTestUtil.createUserModel(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                GenderEnum.F,
                updatedDob);

        UserDto updateDto = UserTestUtil.createApiGatewayUserDto(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                GenderEnum.F,
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
        assertThat(result.getGender()).isEqualTo(GenderEnum.F);
        verify(userService, times(1)).updateUser(eq(updateModel));

    }

    @Test
    public void shouldReturnUserGivenUserId() throws ExecutionException, InterruptedException {

        User persistedModel = UserTestUtil.createUserModel(userId,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M,
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
        assertThat(result.getGender()).isEqualTo(persistedModelGenderEnum);
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
                GenderEnum.M,
                persistedModelDob);

        given(messageService.getMessage(MessageCodes.SUCCESS)).willReturn(successMsg);
        given(userService.findOne(userId)).willAnswer(invocationOnMock -> CompletableFuture.completedFuture(persistedModel));
        given(authorizationApiService.deleteUserCredentials(persistedModel.getEmail(), MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY))).willAnswer(invocation -> CompletableFuture.completedFuture(new AuthResponseDto()));

        doAnswer(invocationOnMock -> CompletableFuture.completedFuture(null)).when(userService)
                .deleteById(userId);

        ResponseEntity responseEntity = (ResponseEntity) userManagementFacade.deleteByUserId(userId).get();

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);
        String message = TestUtil.getResponseDtoMessage(responseEntity);
        assertThat(successMsg).isEqualToIgnoringCase(message);

        verify(userService).deleteById(userId);
        verify(userService).findOne(userId);
        verify(authorizationApiService).deleteUserCredentials(persistedModel.getEmail(), MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY));
        verify(messageService).getMessage(MessageCodes.SUCCESS);

    }


    @Test
    @Ignore
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
                GenderEnum.M, null);

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
                GenderEnum.M, null);

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
                GenderEnum.F, null);

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

    @Test
    public void shouldThrowInvalidDataExceptionGivenEmptyEmailOnRequestPasswordReset () throws ExecutionException, InterruptedException {

        String email = "";
        String errMsg = "to data is null or empty";

        String[] replacements = new String[]{"to"};

        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements )).willReturn(errMsg);


        // When
        try {
            userManagementFacade.requestPasswordReset(email, mdcAdapter).get();
        } catch (Throwable ex) {
            Throwable businessEx = CommonUtils.extractBusinessException(ex);
            Assertions.assertThat(businessEx instanceof InvalidDataException).isTrue();
            verify(messageService).getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements );

        }


    }

    @Test
    public void shouldReturnOkGivenValidEmailOnRequestPasswordReset () throws ExecutionException, InterruptedException {

        User user = UserTestUtil.createUserModel(1L,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, null);

        String message = "OK";
        String jwt = "jwt_token";

        AuthRequestDto authRequestDto = new AuthRequestDto();
        authRequestDto.setTokenType(TokenType.PasswordReset);
        authRequestDto.setEmail(persistedModelEmail);

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(jwt);

        EmailRequestDto emailRequestDto = new EmailRequestDto();
        emailRequestDto.setTo(persistedModelEmail);
        //emailRequestDto.setToken(tokenDto.getToken());
        //emailRequestDto.setEmailType(EmailType.PasswordReset);


        // Given
        given(messageService.getMessage(MessageCodes.SUCCESS)).willReturn(message);
        given(userService.findOneByEmail(persistedModelEmail, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(user));
        given(authorizationApiService.requestToken(authRequestDto, EVENT, TRACE_ID))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(tokenDto));

        given(emailApiService.sendEmailByTemplate(EmailTemplates.PASSWORD_RESET_TEMPLATE, emailRequestDto, EVENT, TRACE_ID))
                .willAnswer(invocationOnMock ->{
                    EmailResponseDto emailResponseDto = new EmailResponseDto();
                    return CompletableFuture.completedFuture(emailResponseDto);
                });
        // When
        ResponseEntity responseEntity = userManagementFacade.requestPasswordReset(persistedModelEmail, mdcAdapter).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        assertThat(httpStatus).isEqualTo(HttpStatus.OK);

        verify(messageService).getMessage(MessageCodes.SUCCESS);
        verify(userService).findOneByEmail(persistedModelEmail, mdcAdapter);
        verify(authorizationApiService).requestToken(authRequestDto, EVENT, TRACE_ID);
        verify(emailApiService).sendEmailByTemplate(EmailTemplates.PASSWORD_RESET_TEMPLATE, emailRequestDto, EVENT, TRACE_ID);
    }

    @Test
    public void shouldReturn400BadRequestGivenEmptyEmailOrPasswordOnPasswordReset () throws ExecutionException, InterruptedException {

        String email = "";
        String errMsg = "to data is null or empty";

        String[] replacements = new String[]{"to or password"};

        UserDto userDto = new UserDto();
        userDto.setEmail(email);
        userDto.setPassword(persistedModelPassword);
        userDto.setConfirmedPassword(persistedModelConfirmedPassword);


        given(messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements )).willReturn(errMsg);

        // When
         ResponseEntity responseEntity = userManagementFacade.resetPassword(userDto, mdcAdapter).get();
         HttpStatus httpStatus = responseEntity.getStatusCode();
         assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageService).getMessage(MessageCodes.NULL_OR_EMPTY_DATA, replacements );

    }

    @Test
    @Ignore
    public void shouldReturn400BadRequestGivenInvalidPasswordOnPasswordReset () throws ExecutionException, InterruptedException {


        String errMsg = "inavlid to or password";

        UserDto userDto = new UserDto();
        userDto.setEmail(persistedModelEmail);
        userDto.setPassword(persistedModelPassword);
        userDto.setConfirmedPassword(persistedModelConfirmedPassword);

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(false);


        given(messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD)).willReturn(errMsg);
        given(passwordService.checkPasswordValidity(userDto.getPassword() )).willReturn(ruleResult);


        // When
        ResponseEntity responseEntity = userManagementFacade.resetPassword(userDto, mdcAdapter).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        assertThat(httpStatus).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(messageService).getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
        verify(passwordService).checkPasswordValidity(userDto.getPassword());


    }

    @Test
    public void shouldReturn404NotFoundGivenEmailForNonExistentUserOnPasswordReset () throws ExecutionException, InterruptedException {


        String errMsg = "user not found";

        UserDto userDto = new UserDto();
        userDto.setEmail("notfound@quantal.com");
        userDto.setPassword(persistedModelPassword);
        userDto.setConfirmedPassword(persistedModelConfirmedPassword);

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(true);

        String[] replacements = new String[]{User.class.getSimpleName()};

        given(messageService.getMessage(MessageCodes.NOT_FOUND, replacements)).willReturn(errMsg);
        given(userService.findOneByEmail(userDto.getEmail(), mdcAdapter ))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(null));
        given(passwordService.checkPasswordValidity(userDto.getPassword() )).willReturn(ruleResult);

        // When
        ResponseEntity responseEntity = userManagementFacade.resetPassword(userDto, mdcAdapter).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        assertThat(httpStatus).isEqualTo(HttpStatus.NOT_FOUND);
        verify(messageService).getMessage(MessageCodes.NOT_FOUND, replacements);
        verify(passwordService).checkPasswordValidity(userDto.getPassword());
        verify(userService).findOneByEmail(userDto.getEmail(), mdcAdapter);
    }




    @Test
    public void shouldReturnOkGivenValidEmailOnPasswordReset () throws ExecutionException, InterruptedException {

        String newPassword = "newPassword";
        UserDto userDto = new UserDto();
        userDto.setEmail(persistedModelEmail);
        userDto.setPassword(persistedModelPassword);
        userDto.setConfirmedPassword(persistedModelConfirmedPassword);

        User user = UserTestUtil.createUserModel(1L,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                GenderEnum.M, null);

        User updatedUser = UserTestUtil.createUserModel(1L,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                newPassword,
                GenderEnum.M, null);

        String message = "OK";
        String jwt = "jwt_token";

        AuthRequestDto authRequestDto = new AuthRequestDto();
        authRequestDto.setTokenType(TokenType.Access);
        authRequestDto.setEmail(persistedModelEmail);

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(jwt);

        RuleResult ruleResult = new RuleResult();
        ruleResult.setValid(true);

        EmailRequestDto emailRequestDto = new EmailRequestDto();
        emailRequestDto.setTo(persistedModelEmail);
        emailRequestDto.setToken(tokenDto.getToken());
        emailRequestDto.setEmailType(EmailType.PasswordReset);


        // Given
        given(messageService.getMessage(MessageCodes.SUCCESS)).willReturn(message);
        given(userService.findOneByEmail(persistedModelEmail, mdcAdapter))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(user));
        given(authorizationApiService.requestToken(authRequestDto, "EVENT", "TRACE_ID"))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(tokenDto));

        given(authorizationApiService.deleteAllTokens(user.getEmail(), MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(new AuthResponseDto()));

        given(userService.saveOrUpdate(updatedUser))
                .willAnswer(invocationOnMock -> CompletableFuture.completedFuture(updatedUser));

        given(passwordService.checkPasswordValidity(userDto.getPassword()))
                .willReturn(ruleResult);
        // When
        ResponseEntity responseEntity = userManagementFacade.resetPassword(userDto, mdcAdapter).get();
        HttpStatus httpStatus = responseEntity.getStatusCode();
        assertThat(httpStatus).isEqualTo(HttpStatus.OK);

        verify(messageService).getMessage(MessageCodes.SUCCESS);
        verify(userService).findOneByEmail(persistedModelEmail, mdcAdapter);
        verify(userService).saveOrUpdate(updatedUser);
        verify(authorizationApiService).requestToken(authRequestDto, "EVENT", "TRACE_ID");
        verify(authorizationApiService).deleteAllTokens(user.getEmail(), MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY));
        verify(passwordService).checkPasswordValidity(userDto.getPassword());
    }


}
