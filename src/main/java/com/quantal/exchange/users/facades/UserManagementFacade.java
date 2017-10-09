package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.enums.EmailType;
import com.quantal.exchange.users.enums.TokenType;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.api.EmailApiService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.shared.dto.ResponseDto;
import com.quantal.shared.facades.AbstractBaseFacade;
import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.shared.objectmapper.OrikaBeanMapper;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.shared.util.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.passay.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserManagementFacade extends AbstractBaseFacade {

  private final UserService userService;
  private final GiphyApiService giphyApiService;
  private final MessageService messageService;
  private final XLogger logger = XLoggerFactory.getXLogger(this.getClass().getName());
  private final AuthorizationApiService authorizationApiService;
  private final EmailApiService emailApiService;
  private final PasswordService passwordService;

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

  public CompletableFuture<ResponseEntity> save(UserDto userDto){
      User userToCreate = toModel(userDto, User.class);
      UserDto createdDto = new UserDto();
      logger.debug("creating user: user ", userDto);
      AuthRequestDto authRequestDto = new AuthRequestDto();
      authRequestDto.setEmail(userDto.getEmail());
       return userService
                .createUser(userToCreate)
                .thenApply(created -> {
                    nullSkippingMapper.map(created, createdDto);
                    logger.debug("created user: user ", userDto);
                    return createdDto;
                })
              .thenApply(user -> authorizationApiService.requestUserCredentials(authRequestDto))
              .thenCompose(res -> res)
              .thenApply(credential -> authorizationApiService.requestToken(authRequestDto))
              .thenCompose(res -> res)
              .thenApply(token -> {
                  createdDto.setToken(token.getToken());
                  ResponseEntity responseEntity = toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
                  return responseEntity;
              });
              /*.exceptionally( ex -> {
                  ResponseEntity responseEntity = toRESTResponse(null,
                          messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR),
                          HttpStatus.INTERNAL_SERVER_ERROR);
                  Exception businessEx = CommonUtils.extractBusinessException(ex);
                  if (businessEx instanceof AlreadyExistsException) {
                      responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.CONFLICT);
                  } else if (businessEx instanceof NullPointerException) {
                      responseEntity = toRESTResponse(null,
                              messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED,
                                      new String[]{User.class.getSimpleName()}),
                              HttpStatus.BAD_REQUEST);
                  } else if (businessEx instanceof PasswordValidationException) {
                      responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.BAD_REQUEST);
                  } else if (businessEx instanceof HttpException) {
                      HttpStatus status = HttpStatus.valueOf(((HttpException) businessEx).code());
                      responseEntity = toRESTResponse(null, businessEx.getMessage(), status);
                  }
                  ;
                  logger.debug("Error creating user:", ex);
                  return responseEntity;
              });*/

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
      logger.info("deleting user identified by {}", userId);
        CompletableFuture<?> userCompletableFuture = userService.findOne(userId);
            return userCompletableFuture.thenCombine(userService.deleteById(userId),  (user, deleted) -> user)
                    .thenApply(user -> {
                        logger.debug("requesting authorization service to delete {} user credentials", ((User)user).getEmail());
                        return authorizationApiService.deleteUserCredentials(((User)user).getEmail());
                    })
                    .thenApply(ret -> {
                        ResponseEntity<?> responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.SUCCESS));
                        logger.info("user identified by {} deleted successfully", userId);
                        return responseEntity;
                    });
    }

    public CompletableFuture<ResponseEntity> requestPasswordReset(String email){

        if (StringUtils.isEmpty(email)) {
            ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{email}), HttpStatus.BAD_REQUEST);
            return CompletableFuture.completedFuture(responseEntity);
        }

        AuthRequestDto authRequestDto = new AuthRequestDto();
        authRequestDto.setTokenType(TokenType.PasswordReset);
        authRequestDto.setEmail(email);
        logger.debug("requesting password reset token for {} ...", email);
           return userService.findOneByEmail(email)
                   .thenApply(user -> authorizationApiService.requestToken(authRequestDto))
                   .thenCompose(tokenDto -> tokenDto)
                   .thenCompose(tokenDto -> {
                       logger.debug("token request success: token:  {} ", tokenDto.getToken());
                       EmailRequestDto emailRequestDto = new EmailRequestDto();
                       emailRequestDto.setEmail(email);
                       emailRequestDto.setToken(tokenDto.getToken());
                       emailRequestDto.setEmailType(EmailType.PasswordReset);

                       logger.debug("sending password reset email to {}", email);
                       return emailApiService.sendEmail(emailRequestDto);
                   })
                   .thenApply(emailResponseDto -> {
                    String message = messageService.getMessage(MessageCodes.SUCCESS);
                    logger.debug("password reset email sent to {} successfully", email);
                    ResponseEntity responseEntity = toRESTResponse(null, message);
                    return responseEntity;
                   })
                   .exceptionally( ex -> {
                       ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                       logger.debug("Error requesting password reset. ",ex);
                       return  responseEntity;
                   });
    }

    public CompletableFuture<ResponseEntity> resetPassword(UserDto userDto){
        logger.debug("resetting password for {}", userDto.getEmail());
        if (StringUtils.isEmpty(userDto.getEmail()) || StringUtils.isEmpty(userDto.getPassword())){
            String message =  messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"email or password"});
            ResponseEntity responseEntity = toRESTResponse(null,message, HttpStatus.BAD_REQUEST);
            logger.debug(message);
            return CompletableFuture.completedFuture(responseEntity);
        }

        logger.debug("checking password validity .. ");
        RuleResult ruleResult = passwordService.checkPasswordValidity(userDto.getPassword());
        if(!ruleResult.isValid()){
            String message =  messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
            String validationErrMsg = passwordService.getPasswordValidationCheckErrorMessages(ruleResult, null);
            logger.debug("{} {}", message, validationErrMsg);
            return CompletableFuture.completedFuture(responseEntity);
        }

        logger.debug("password validated ... ");
        logger.debug("finding user identified by {}", userDto.getEmail());
        return userService.
                findOneByEmail(userDto.getEmail())
                .thenApply(user -> {
                    if(user == null){
                      throw logger.throwing(new NotFoundException(""));
                    }
                    String newPassword = passwordService.hashPassword(userDto.getPassword());
                    user.setPassword(newPassword);
                    logger.debug("updating password. user: {}", user);
                    return userService.saveOrUpdate(user);
                })
                .thenCompose(userCompletableFuture -> userCompletableFuture)
                .thenApply(updateUser -> {
                    logger.debug("deleting all tokens for user {} ", updateUser);
                    return authorizationApiService.deleteAllTokens(updateUser.getEmail());
                })
                .thenApply(authResponseDtoCompletableFuture -> {
                    logger.debug("all tokens deleted for user identified  by {} ", userDto.getEmail());
                    AuthRequestDto authRequestDto = new AuthRequestDto();
                    authRequestDto.setEmail(userDto.getEmail());
                    authRequestDto.setTokenType(TokenType.Access);
                    logger.debug("requesting new access token for user identified by {}", userDto.getEmail());
                    return authorizationApiService.requestToken(authRequestDto);
                })
                .thenCompose(tokenDtoCompletableFuture -> tokenDtoCompletableFuture)
                .thenApply(tokenDto -> {
                    logger.debug("access token granted for {}", userDto.getEmail());
                    String message = messageService.getMessage(MessageCodes.SUCCESS);
                    ResponseEntity responseEntity = toRESTResponse(tokenDto, message);
                    logger.debug("password successfully reset for {}", userDto.getEmail());
                    return responseEntity;
                })
                .exceptionally(ex -> {
                    Throwable busEx = CommonUtils.extractBusinessException(ex);
                    String message =  messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR);
                    ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.INTERNAL_SERVER_ERROR);
                    if (busEx instanceof NotFoundException){
                        message =  messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()});
                        responseEntity = toRESTResponse(null, message, HttpStatus.NOT_FOUND);
                        logger.debug(message+". user email: {}  -  HttpCode: {}", userDto.getEmail(), HttpStatus.NOT_FOUND);
                        return responseEntity;
                    }
                    logger.debug("Error resetting user password. ", ex);
                    return responseEntity;
                });
    }

  public CompletableFuture<String> getFunnyCat(){
    //String result = "";
    //String result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC");
    CompletableFuture<String> result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC").thenApply((res) -> {
      return res;
    });
    return result;
  }

}
