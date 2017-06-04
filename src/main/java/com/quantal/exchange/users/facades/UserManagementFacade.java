package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.exceptions.PasswordValidationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final Logger logger = LoggerFactory.getLogger(UserManagementFacade.class);

  @Autowired
  public UserManagementFacade(UserService userService,
                              GiphyApiService giphyApiService,
                              MessageService messageService,
                              OrikaBeanMapper orikaBeanMapper,
                              NullSkippingOrikaBeanMapper nullSkippingOrikaBeanMapper) {
    super(orikaBeanMapper, nullSkippingOrikaBeanMapper);
    this.userService = userService;
    this.giphyApiService = giphyApiService;
    this.messageService = messageService;
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
                            messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR,
                                    new String[]{User.class.getSimpleName()}),
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

  public CompletableFuture<String> getFunnyCat(){
    //String result = "";
    //String result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC");
    CompletableFuture<String> result = giphyApiService.getGiphy("funny+cat", "dc6zaTOxFJmzC").thenApply((res) -> {
      return res;
    });
    return result;
  }
}
