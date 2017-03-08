package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserManagementFacade extends AbstractBaseFacade {

  private UserService userService;

  @Autowired
  public UserManagementFacade(UserService userService) {
    this.userService = userService;
  }

  public ResponseEntity<?> saveOrUpdateUser(UserDto userDto){

    User userToCreate = toModel(userDto, User.class);
    User created  = userService.saveOrUpdate(userToCreate);
    UserDto createdDto = toDto(created, UserDto.class);
    return toRESTResponse(createdDto);
  }
}
