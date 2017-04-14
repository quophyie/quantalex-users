package com.quantal.exchange.users.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.exchange.users.jsonviews.UserViews;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 08/03/2017.
 */

@RestController
@RequestMapping("/users/")
public class UserController {

  private UserManagementFacade userManagementFacade;

  @Autowired
  public UserController(UserManagementFacade userManagementFacade) {
    this.userManagementFacade = userManagementFacade;
  }

  @JsonView(UserViews.CreatedUserView.class)
  @PostMapping(value="", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createUser(@RequestBody UserDto userDto){
    return userManagementFacade.save(userDto);
  }

  @GetMapping(value="")
  public CompletableFuture<String> getFunnyCatAsync(){
    return userManagementFacade.getFunnyCat();
  }


}
