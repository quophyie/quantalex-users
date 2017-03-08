package com.quantal.exchange.users.services.implementations;

import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserServiceImpl implements UserService {

  private UserRepository userRepository;

  @Autowired
  public UserServiceImpl(UserRepository userRepository){
   this.userRepository = userRepository;
  }
  @Override
  public User saveOrUpdate(User user) {
    user.setJoinDate(LocalDate.now());
    return userRepository.save(user);
  }
}
