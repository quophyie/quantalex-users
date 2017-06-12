package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.enums.EmailType;
import com.quantal.exchange.users.enums.TokenType;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.api.AuthorizationService;
import com.quantal.exchange.users.services.api.EmailService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserManagementFacade extends AbstractBaseFacade {

  private final UserService userService;
  private final GiphyApiService giphyApiService;
  private final MessageService messageService;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final AuthorizationService authorizationService;
  private final EmailService emailService;

  @Autowired
  public UserManagementFacade(UserService userService,
                              GiphyApiService giphyApiService,
                              MessageService messageService,
                              @Qualifier("orikaBeanMapper")
                                      OrikaBeanMapper orikaBeanMapper,
                              @Qualifier("nullSkippingOrikaBeanMapper")
                                      NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper,
                              AuthorizationService authorizationService,
                              EmailService emailService) {
    super(orikaBeanMapper, nullSkippingOrikaBeanMapper);
    this.userService = userService;
    this.giphyApiService = giphyApiService;
    this.messageService = messageService;
    this.authorizationService = authorizationService;
    this.emailService = emailService;
  }

  private UserDto createUserDto(User user,UserDto userDto){
       userDto = toDto(user, UserDto.class);
       return  userDto;
  }

  public CompletableFuture<ResponseEntity> save(UserDto userDto){
      User userToCreate = toModel(userDto, User.class);
      UserDto createdDto = new UserDto();
      logger.debug("creating user: user ", userDto);
       return userService
                .createUser(userToCreate)
                .thenApply(created -> {
                    //createUserDto(created, createdDto);
                    nullSkippingMapper.map(created, createdDto);
                    //createdDto = toDto(created, UserDto.class);
                   // ResponseEntity responseEntity =toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
                   // return responseEntity;
                    logger.debug("created user: user ", userDto);
                    return createdDto;
                })
              .thenApply(user -> userService.requestApiGatewayUserCredentials(user.getEmail()))
              .thenCompose(res -> res)
              .thenApply(credential -> userService.createJwt(credential.getKey()))
              .thenApply(token -> {
                  createdDto.setToken(token);
                  ResponseEntity responseEntity = toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
                  return responseEntity;
              })
              .exceptionally( ex -> {
                    ResponseEntity responseEntity =  toRESTResponse(null,
                            messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                    Exception businessEx = CommonUtils.extractBusinessException(ex);
                   if (businessEx instanceof AlreadyExistsException){
                       responseEntity =  toRESTResponse(null, businessEx.getMessage(), HttpStatus.CONFLICT);
                    } else if (businessEx instanceof NullPointerException){
                       responseEntity =  toRESTResponse(null,
                               messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED,
                                       new String[]{User.class.getSimpleName()}),
                               HttpStatus.BAD_REQUEST);
                    }  else if (businessEx instanceof PasswordValidationException) {
                       responseEntity = toRESTResponse(null, businessEx.getMessage(), HttpStatus.BAD_REQUEST);
                   }

                   logger.debug("Error creating user:", ex);

                    return responseEntity;
                });

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

    public CompletableFuture<ResponseEntity> findUserById(Long userId){
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

            return userService.deleteById(userId)
                    .thenApply(ret -> {
                        ResponseEntity<?> responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.SUCCESS));
                        return responseEntity;
                    })
                   // .thenCompose(responseEntity -> responseEntity)
                    .exceptionally(ex -> {
                        CompletableFuture exFuture = new CompletableFuture();
                        ResponseEntity responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                        if (ex.getCause() instanceof NotFoundException) {
                            responseEntity = toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
                            return responseEntity;
                        }
                        return  responseEntity;
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
                   .thenApply(user -> authorizationService.requestToken(authRequestDto))
                   .thenCompose(tokenDto -> tokenDto)
                   .thenCompose(tokenDto -> {
                       logger.debug("token request success: token:  {} ", tokenDto.getToken());
                       EmailRequestDto emailRequestDto = new EmailRequestDto();
                       emailRequestDto.setEmail(email);
                       emailRequestDto.setToken(tokenDto.getToken());
                       emailRequestDto.setEmailType(EmailType.PasswordReset);

                       logger.debug("sending password reset email to {}", email);
                       return emailService.sendEmail(emailRequestDto);
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

  public CompletableFuture<String> getFunnyCat(){
    //String result = "";
    //String result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC");
    CompletableFuture<String> result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC").thenApply((res) -> {
      return res;
    });
    return result;
  }
}
