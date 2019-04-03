package com.quantal.exchange.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.constants.Events;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.PasswordMatchType;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.jsonviews.LoginView;
import com.quantal.exchange.users.jsonviews.UserViews;
import com.quantal.exchange.users.validators.password.PasswordMatches;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.controller.BaseControllerAsync;
import com.quantal.javashared.jsonviews.DefaultJsonView;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.quantal.exchange.users.constants.Events.USER_PASSWORD_RESET_REQUEST;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;

/**
 * Created by dman on 08/03/2017.
 */

@RestController
@RequestMapping("/v1/users")
@Slf4j
public class UserController extends BaseControllerAsync {

  private UserManagementFacade userManagementFacade;

  private ObjectMapper objectMapper;
  @Autowired
  private ExecutorService taskExecutor;

  @Autowired
  public UserController(UserManagementFacade userManagementFacade,
                        ObjectMapper objectMapper) {
    this.userManagementFacade = userManagementFacade;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value="", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CompletableFuture<ResponseEntity> createUser(@RequestBody
                                                        @Validated
                                                      @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                       UserDto userDto){
    MDC.put(CommonConstants.EVENT_KEY, Events.USER_CREATE);
    return userManagementFacade
            .save(userDto, MDC.getMDCAdapter())
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @PutMapping(value="/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public CompletableFuture<ResponseEntity> updateUser(@PathVariable("userId") Long userId,
                                                      @RequestBody
                                                      @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                              UserDto userDto){
    MDC.put(EVENT_KEY, Events.USER_UPDATE);
    return userManagementFacade
            .updateUser(userId, userDto)
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @GetMapping(value="/{userId}")
  public CompletableFuture<ResponseEntity> findUserbyId(@PathVariable Long userId){
    MDC.put(EVENT_KEY, Events.USER_SEARCH);
    return userManagementFacade
            .findUserById(userId)
            .thenApply(responseEntity -> applyJsonView(responseEntity, UserViews.CreatedAndUpdatedUserView.class, objectMapper));
  }

  @DeleteMapping(value="/{userId}")
  public CompletableFuture<?> deleteUserbyId(@PathVariable Long userId){
    MDC.put(EVENT_KEY, Events.USER_DELETE);
    return userManagementFacade.deleteByUserId(userId)
            .thenApply(responseEntity -> applyJsonView((ResponseEntity) responseEntity, DefaultJsonView.ResponseDtoView.class, objectMapper));
  }

  @PostMapping(value="/forgotten-password")
  //@Async
  public CompletableFuture<?> forgottenPassword(@RequestBody
                                                 @PasswordMatches (passwordMatchType = PasswordMatchType.ALLOW_NULL_MATCH)
                                                          UserDto userDto){
    MDC.put(EVENT_KEY,USER_PASSWORD_RESET_REQUEST);
    return userManagementFacade.requestPasswordReset(userDto.getEmail(), MDC.getMDCAdapter())
            .thenApply( responseEntity -> applyJsonView(responseEntity, DefaultJsonView.ResponseDtoView.class, objectMapper));
  }

  @PostMapping(value="/reset-password")
  public CompletableFuture<?> resetPassword(@RequestBody UserDto userDto){
    MDC.put(CommonConstants.EVENT_KEY, Events.USER_PASSWORD_RESET);
    return userManagementFacade.resetPassword(userDto, MDC.getMDCAdapter())
            .thenApply(responseEntity -> applyJsonView(responseEntity, LoginView.LoginResponse.class, objectMapper));
  }

  @GetMapping(value="")
  public CompletableFuture<String> getFunnyCatAsync(){
    return userManagementFacade.getFunnyCat();
  }


}
