package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
  public UserManagementFacade(UserService userService, GiphyApiService giphyApiService, MessageService messageService) {
    this.userService = userService;
    this.giphyApiService = giphyApiService;
    this.messageService = messageService;
  }

  public ResponseEntity<?> save(UserDto userDto){

    User userToCreate = toModel(userDto, User.class);
    User created  = userService.saveOrUpdate(userToCreate);
    UserDto createdDto = toDto(created, UserDto.class);
    return toRESTResponse(createdDto, messageService.getMessage(MessageCodes.ENTITY_CREATED, new String[]{User.class.getSimpleName()}), HttpStatus.CREATED);
  }

  public ResponseEntity<?>  update(Long userId, UserDto userUpdateDto){

    String message = "User not found";
    if (userUpdateDto == null){
      return toRESTResponse(userUpdateDto, "User not modified", HttpStatus.NOT_MODIFIED);
    }
    User userToUpdate = userService.findOne(userId);

    if (userToUpdate == null){
      return toRESTResponse(userUpdateDto, message, HttpStatus.NOT_FOUND);
    }
    User userToSave = toModel(userUpdateDto, userToUpdate, false);
    User updated  = userService.saveOrUpdate(userToSave);
    UserDto updatedDto = toDto(updated, UserDto.class);
    return toRESTResponse(updatedDto, messageService.getMessage(MessageCodes.ENTITY_UPDATED, new String[]{User.class.getSimpleName()}), HttpStatus.OK);
    //return toRESTResponse(updatedDto, "User updated");
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
