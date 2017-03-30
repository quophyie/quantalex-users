package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Null;
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
  public UserManagementFacade(UserService userService, GiphyApiService giphyApiService, MessageService messageService) {
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
        return toRESTResponse(userDto, aee.getMessage(), HttpStatus.CONFLICT);
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
          User updated = userService.updateUser(userId, userUpdateModel);
          UserDto updatedDto = toDto(updated, UserDto.class);
          return toRESTResponse(updatedDto, messageService.getMessage(MessageCodes.ENTITY_UPDATED, new String[]{User.class.getSimpleName()}), HttpStatus.OK);
      } catch (NotFoundException npe) {
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
