package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.UserDto;
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

  private UserService userService;
  private final GiphyApiService giphyApiService;

  @Autowired
  public UserManagementFacade(UserService userService, GiphyApiService giphyApiService) {
    this.userService = userService;
    this.giphyApiService = giphyApiService;
  }

  public ResponseEntity<?> saveOrUpdateUser(UserDto userDto){

    User userToCreate = toModel(userDto, User.class);
    User created  = userService.saveOrUpdate(userToCreate);
    UserDto createdDto = toDto(created, UserDto.class);
    return toRESTResponse(createdDto, "User created successfully", HttpStatus.CREATED);
  }

  public ResponseEntity<?>  update(UserDto userDto){

    String message = "User not found";
    if (userDto == null){
      return toRESTResponse(userDto, message, HttpStatus.NOT_FOUND);
    }
    User userToUpdate = userService.findOneByEmail(userDto.getEmail());

    if (userToUpdate == null){
      return toRESTResponse(userDto, message, HttpStatus.NOT_FOUND);
    }
    User userToSave = toModel(userDto, userToUpdate, false);
    User updated  = userService.saveOrUpdate(userToSave);
    UserDto updatedDto = toDto(updated, UserDto.class);
    return toRESTResponse(updatedDto, null);
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
