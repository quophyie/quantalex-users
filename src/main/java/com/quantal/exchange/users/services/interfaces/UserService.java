package com.quantal.exchange.users.services.interfaces;

import com.quantal.exchange.users.models.User;
import com.quantal.shared.services.interfaces.RepositoryService;

import java.util.List;

/**
 * Created by dman on 08/03/2017.
 */
public interface UserService extends RepositoryService<User, Long> {

  User createUser(User user);
  User saveOrUpdate(User user);
  User findOneByEmail(String email);
  //User findOne(Long userid);
  void deleteById(Long userid);
  User updateUser(User updateData);
  Long countByEmailIgnoreCase(String email);
  List<User> findAllByEmailIgnoreCase(String email);
}
