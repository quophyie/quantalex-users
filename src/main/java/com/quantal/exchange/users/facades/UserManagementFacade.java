package com.quantal.exchange.users.facades;

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

  public ResponseEntity<?> save(UserDto userDto){

    try {
        User userToCreate = toModel(userDto, User.class);
        User created = userService.createUser(userToCreate);
        UserDto createdDto = toDto(created, UserDto.class);
        return toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
    } catch (AlreadyExistsException aee) {
        return toRESTResponse(null, aee.getMessage(), HttpStatus.CONFLICT);
    } catch (NullPointerException npe) {
        return toRESTResponse(null, messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED, new String[]{User.class.getSimpleName()}), HttpStatus.BAD_REQUEST);
    }
  }

  public ResponseEntity<?> updateUser(Long userId, UserDto userUpdateDto){

      if (userUpdateDto == null) {
          return toRESTResponse(null, messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED, new String[]{User.class.getSimpleName()}), HttpStatus.BAD_REQUEST);
      }
      try {
          User userUpdateModel = toModel(userUpdateDto, new User(), false);
          userUpdateModel.setId(userId);
          User updated = userService.updateUser(userUpdateModel);
          UserDto updatedDto = toDto(updated, UserDto.class);
          return toRESTResponse(updatedDto, messageService.getMessage(MessageCodes.ENTITY_UPDATED, new String[]{User.class.getSimpleName()}), HttpStatus.OK);
      } catch (NotFoundException npe) {
          return toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
      }
      catch (AlreadyExistsException aee) {
          return toRESTResponse(null, aee.getMessage(), HttpStatus.CONFLICT);
      }
  }

    public ResponseEntity<?> findUserById(Long userId){
        User user = userService.findOne(userId);
        UserDto userDto = toModel(user, UserDto.class);
        if (user == null) {
            return toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
        }

        return toRESTResponse(userDto, messageService.getMessage(MessageCodes.SUCCESS));
    }

    public ResponseEntity<?> deleteByUserId(Long userId) {
        try {
            userService.deleteById(userId);
            return toRESTResponse(null, messageService.getMessage(MessageCodes.SUCCESS));
        } catch (NotFoundException nfe){
            return toRESTResponse(null, messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}), HttpStatus.NOT_FOUND);
        }
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
