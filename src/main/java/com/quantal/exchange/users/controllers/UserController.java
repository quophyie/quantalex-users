package com.quantal.exchange.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.PasswordMatchType;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.jsonviews.UserViews;
import com.quantal.exchange.users.validators.password.PasswordMatches;
import com.quantal.shared.controller.BaseControllerAsync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 08/03/2017.
 */

@RestController
@RequestMapping("/users")
public class UserController extends BaseControllerAsync {

  private UserManagementFacade userManagementFacade;

  private ObjectMapper objectMapper;

  @Autowired
  public UserController(UserManagementFacade userManagementFacade,
                        ObjectMapper objectMapper) {
    this.userManagementFacade = userManagementFacade;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value="/", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CompletableFuture<ResponseEntity> createUser(@RequestBody
                                                        @Validated
                                                      @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                       UserDto userDto){
    return userManagementFacade
            .save(userDto)
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @PutMapping(value="/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CompletableFuture<ResponseEntity> updateUser(@PathVariable("userId") Long userId,
                                                      @RequestBody
                                                      @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                              UserDto userDto){
    return userManagementFacade
            .updateUser(userId, userDto)
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @GetMapping(value="/{userId}")
  public CompletableFuture<ResponseEntity> findUserbyId(@PathVariable Long userId){
    return userManagementFacade
            .findUserById(userId)
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @DeleteMapping(value="/{userId}")
  public CompletableFuture<?> deleteUserbyId(@PathVariable Long userId){
    return userManagementFacade.deleteByUserId(userId);
  }

  @PostMapping(value="/forgotten-password")
  public CompletableFuture<?> forgottenPassword(@RequestBody
                                                 @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                          UserDto userDto){
    return userManagementFacade.requestPasswordReset(userDto.getEmail());
  }

  @PostMapping(value="/reset-password")
  public CompletableFuture<?> resetPassword(@RequestBody UserDto userDto){
    return userManagementFacade.resetPassword(userDto);
  }

  @GetMapping(value="/")
  public CompletableFuture<String> getFunnyCatAsync(){
    return userManagementFacade.getFunnyCat();
  }


}
