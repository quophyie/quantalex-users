package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.EmailTemplates;
import com.quantal.exchange.users.constants.Events;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.TokenType;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.api.EmailApiService;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.javashared.annotations.logger.InjectLogger;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.dto.ResponseDto;
import com.quantal.javashared.facades.AbstractBaseFacade;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.passay.RuleResult;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.quantal.javashared.constants.CommonConstants.EMAIL_KEY;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.STATUS_CODE_KEY;
import static com.quantal.javashared.constants.CommonConstants.SUB_EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.USER_ID_KEY;
import static com.quantal.javashared.constants.CommonConstants.USER_KEY;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserManagementFacade extends AbstractBaseFacade {

  private final UserService userService;
  private final GiphyApiService giphyApiService;
  private final MessageService messageService;

  private final AuthorizationApiService authorizationApiService;
  private final EmailApiService emailApiService;
  private final PasswordService passwordService;
  @Autowired
  private ExecutorService taskExecutor;


    //private final XLogger logger = XLoggerFactory.getXLogger(this.getClass().getName());
  @InjectLogger
  private QuantalLogger logger;

  @Autowired

  public UserManagementFacade(UserService userService,
                              GiphyApiService giphyApiService,
                              MessageService messageService,
                              @Qualifier("orikaBeanMapper")
                                      OrikaBeanMapper orikaBeanMapper,
                              @Qualifier("nullSkippingOrikaBeanMapper")
                                      NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper,
                              AuthorizationApiService authorizationApiService,
                              EmailApiService emailApiService,
                              PasswordService passwordService) {
    super(orikaBeanMapper, nullSkippingOrikaBeanMapper);
    this.userService = userService;
    this.giphyApiService = giphyApiService;
    this.messageService = messageService;
    this.authorizationApiService = authorizationApiService;
    this.emailApiService = emailApiService;
    this.passwordService = passwordService;
  }

  private UserDto createUserDto(User user,UserDto userDto){
       userDto = toDto(user, UserDto.class);
       return  userDto;
  }

  public CompletableFuture<ResponseEntity> save(UserDto userDto, MDCAdapter mdcAdapter){
      User userToCreate = toModel(userDto, User.class);
      UserDto createdDto = new UserDto();
      final ResponseEntity[] responseEntity = new ResponseEntity[1];

      logger.with(USER_KEY, userDto)
            .with(EVENT_KEY, Events.USER_CREATE)
            .debug("creating user ");
      AuthRequestDto authRequestDto = new AuthRequestDto();
      authRequestDto.setEmail(userDto.getEmail());
       return userService
                .createUser(userToCreate, mdcAdapter)
                .thenApply(created -> {
                    nullSkippingMapper.map(created, createdDto);
                    logger.with(EVENT_KEY, Events.USER_CREATE)
                            .debug("created user: user ", userDto);
                    return createdDto;
                })
              .thenApply(user -> authorizationApiService.requestUserCredentials(authRequestDto))
              .thenCompose(res -> res)
              .thenApply(credential -> authorizationApiService.requestToken(authRequestDto, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)))
              .thenCompose(res -> res)
              .thenApply(token -> {
                  createdDto.setToken(token.getToken());
                   responseEntity[0] = toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
                  return responseEntity[0];
              })
              .thenApply(responseEnt -> {
                EmailRequestDto emailDetails = new EmailRequestDto();
                emailDetails.setTo(userDto.getEmail());
               return emailApiService.sendEmailByTemplate(EmailTemplates.NEW_USER_TEMPLATE,emailDetails);
              })
               .thenCompose(emailResponseDtoCompletableFuture -> emailResponseDtoCompletableFuture)
               .thenApply(emailResponseDto ->  responseEntity[0]);

  }

  public CompletableFuture<ResponseEntity> updateUser(Long userId, UserDto userUpdateDto){

      if (userUpdateDto == null) {
          ResponseEntity<?> responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED, new String[]{User.class.getSimpleName()}), HttpStatus.BAD_REQUEST);
          return CompletableFuture.completedFuture(responseEntity);
      }
          User userUpdateModel = toModel(userUpdateDto, new User(), false);
          userUpdateModel.setId(userId);
           return userService.updateUser(userUpdateModel)
                  .thenApply(updated -> {
                      UserDto updatedDto = toDto(updated, UserDto.class);
                      return toRESTResponse(updatedDto, messageService.getMessage(MessageCodes.ENTITY_UPDATED, new String[]{User.class.getSimpleName()}), HttpStatus.OK);
                  })
                  .exceptionally( ex -> {
                      ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR, new String[]{User.class.getSimpleName()}), HttpStatus.INTERNAL_SERVER_ERROR);

                      Exception businessEx = CommonUtils.extractBusinessException((Throwable) ex);
                      if (businessEx instanceof AlreadyExistsException) {
                          responseEntity = toRESTResponse(null, ((Throwable)ex).getCause().getMessage(), HttpStatus.CONFLICT);
                      } else if (businessEx instanceof NotFoundException) {
                          responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
                      }  else if (businessEx instanceof PasswordValidationException) {
                          responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.BAD_REQUEST);
                      }

                      return responseEntity;
                  });


  }

    public CompletableFuture<ResponseEntity> findUserById(Long userId) {
        return userService.findOne(userId)
                .thenApply(user -> {
                    UserDto userDto = toModel(user, UserDto.class);
                    ResponseEntity<ResponseDto<User>> responseEntity = null;
                    if (user == null) {
                        responseEntity = (ResponseEntity<ResponseDto<User>>) toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
                        return responseEntity;
                    }

                    responseEntity = (ResponseEntity<ResponseDto<User>>) toRESTResponse(userDto, messageService.getMessage(MessageCodes.SUCCESS));

                    return responseEntity;
                });

    }


    public CompletableFuture<?> deleteByUserId(Long userId) {

        logger.with(EVENT_KEY, Events.USER_DELETE)
              .with(USER_ID_KEY, userId)
              .info("deleting user identified by {}", userId);
        CompletableFuture<?> userCompletableFuture = userService.findOne(userId);
            return userCompletableFuture.thenCombine(userService.deleteById(userId),  (user, deleted) -> user)
                    .thenApply(user -> {
                        logger.with(EVENT_KEY, Events.USER_DELETE)
                                .with(USER_ID_KEY, userId)
                                .with(EMAIL_KEY, ((User)user).getEmail())
                                .debug("requesting authorization service to delete {} user credentials", ((User)user).getEmail());
                        return authorizationApiService.deleteUserCredentials(((User)user).getEmail());
                    })
                    .thenApply(ret -> {
                        ResponseEntity<?> responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.SUCCESS));
                        logger.with(EVENT_KEY, Events.USER_DELETE)
                                .with(USER_ID_KEY, userId)
                                .info("user identified by {} deleted successfully", userId);
                        return responseEntity;
                    });
    }

    public CompletableFuture<ResponseEntity> requestPasswordReset(String email, MDCAdapter mdcAdapter){

        if (StringUtils.isEmpty(email)) {
            ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{email}), HttpStatus.BAD_REQUEST);
            return CompletableFuture.completedFuture(responseEntity);
        }

        AuthRequestDto authRequestDto = new AuthRequestDto();
        authRequestDto.setTokenType(TokenType.PasswordReset);
        authRequestDto.setEmail(email);

        logger.with(SUB_EVENT_KEY, Events.USER_PASSWORD_RESET)
                .with(EMAIL_KEY, email)
                .debug("requesting password reset token for {} ...", email);
           return userService.findOneByEmail(email, mdcAdapter)
                   .thenApply(user -> authorizationApiService.requestToken(authRequestDto, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)))
                   .thenCompose(tokenDto -> tokenDto)
                   .thenCompose(tokenDto -> {
                       logger.with(SUB_EVENT_KEY, Events.USER_PASSWORD_RESET)
                               .with(EMAIL_KEY, email)
                               .with("token", tokenDto.getToken())
                               .debug("token request success: token:  {} ", tokenDto.getToken());
                       EmailRequestDto emailRequestDto = new EmailRequestDto();
                       emailRequestDto.setTo(email);
                       //emailRequestDto.setToken(tokenDto.getToken());
                       //emailRequestDto.setEmailType(EmailType.PasswordReset);

                       logger.with(SUB_EVENT_KEY, Events.USER_PASSWORD_RESET)
                               .with(EMAIL_KEY, email)
                               .debug("sending password reset to to {}", email);
                       return emailApiService.sendEmailByTemplate(EmailTemplates.PASSWORD_RESET_TEMPLATE, emailRequestDto);
                   })
                   .thenApply(emailResponseDto -> {
                    String message = messageService.getMessage(MessageCodes.SUCCESS);
                    logger.with(SUB_EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY, email)
                            .debug("password reset to sent to {} successfully", email);
                    ResponseEntity responseEntity = toRESTResponse(null, message);
                    return responseEntity;
                   })
                   .exceptionally( ex -> {
                       ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                       logger.with(EMAIL_KEY, email)
                               .debug("Error requesting password reset. ",ex);
                       return  responseEntity;
                   });
    }

    public CompletableFuture<ResponseEntity> resetPassword(UserDto userDto){
        logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                .with(EMAIL_KEY,  userDto.getEmail())
                .debug("resetting password for {}", userDto.getEmail());
        if (StringUtils.isEmpty(userDto.getEmail()) || StringUtils.isEmpty(userDto.getPassword())){
            String message =  messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"to or password"});
            ResponseEntity responseEntity = toRESTResponse(null,message, HttpStatus.BAD_REQUEST);
            logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                    .debug(message);
            return CompletableFuture.completedFuture(responseEntity);
        }

        logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                .with(EMAIL_KEY,  userDto.getEmail())
                .debug("checking password validity .. ");
        RuleResult ruleResult = passwordService.checkPasswordValidity(userDto.getPassword());
        if(!ruleResult.isValid()){
            String message =  messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
            String validationErrMsg = passwordService.getPasswordValidationCheckErrorMessages(ruleResult, null);
            logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                    .with(EMAIL_KEY,  userDto.getEmail())
                    .debug("{} {}", message, validationErrMsg);
            return CompletableFuture.completedFuture(responseEntity);
        }

        logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                .with(EMAIL_KEY,  userDto.getEmail())
                .debug("password validated ... ");

        logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                .with(EMAIL_KEY,  userDto.getEmail())
                .debug("finding user identified by {}", userDto.getEmail());

        return userService.
                findOneByEmail(userDto.getEmail(), MDC.getMDCAdapter())
                .thenApply(user -> {
                    if(user == null){
                      throw logger.throwing(new NotFoundException(""));
                    }
                    String newPassword = passwordService.hashPassword(userDto.getPassword());
                    user.setPassword(newPassword);
                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(USER_KEY, user)
                            .debug("updating password. user: {}", user);
                    return userService.saveOrUpdate(user);
                })
                .thenCompose(userCompletableFuture -> userCompletableFuture)
                .thenApply(updateUser -> {
                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY,  userDto.getEmail())
                            .debug("deleting all tokens for user {} ", updateUser);
                    return authorizationApiService.deleteAllTokens(updateUser.getEmail());
                })
                .thenApply(authResponseDtoCompletableFuture -> {
                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY,  userDto.getEmail())
                            .debug("all tokens deleted for user identified  by {} ", userDto.getEmail());

                    AuthRequestDto authRequestDto = new AuthRequestDto();
                    authRequestDto.setEmail(userDto.getEmail());
                    authRequestDto.setTokenType(TokenType.Access);

                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY,  userDto.getEmail())
                            .debug("requesting new access token for user identified by {}", userDto.getEmail());
                    return authorizationApiService.requestToken(authRequestDto, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY));
                })
                .thenCompose(tokenDtoCompletableFuture -> tokenDtoCompletableFuture)
                .thenApply(tokenDto -> {
                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY,  userDto.getEmail())
                            .debug("access token granted for {}", userDto.getEmail());

                    String message = messageService.getMessage(MessageCodes.SUCCESS);
                    ResponseEntity responseEntity = toRESTResponse(tokenDto, message);

                    logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                            .with(EMAIL_KEY,  userDto.getEmail())
                            .debug("password successfully reset for {}", userDto.getEmail());
                    return responseEntity;
                })
                .exceptionally(ex -> {
                    Throwable busEx = CommonUtils.extractBusinessException(ex);
                    String message =  messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR);
                    ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.INTERNAL_SERVER_ERROR);
                    if (busEx instanceof NotFoundException){
                        message =  messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()});
                        responseEntity = toRESTResponse(null, message, HttpStatus.NOT_FOUND);
                        logger.with(EVENT_KEY, Events.USER_PASSWORD_RESET)
                                .with(STATUS_CODE_KEY, HttpStatus.NOT_FOUND)
                                .with(EMAIL_KEY,  userDto.getEmail())
                                .debug(message+". user to: {}  -  HttpCode: {}", userDto.getEmail(), HttpStatus.NOT_FOUND);
                        return responseEntity;
                    }
                    logger.debug("Error resetting user password. ", ex);
                    return responseEntity;
                });
    }

  public CompletableFuture<String> getFunnyCat(){
    CompletableFuture<String> result = giphyApiService
            .getGiphy("funny+cat", "dc6zaTOxFJmzC").thenApply((res) -> res);
    return result;
  }

}
