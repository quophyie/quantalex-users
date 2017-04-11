package com.quantal.exchange.users.services.interfaces;

import com.quantal.exchange.users.models.User;

/**
 * Created by dman on 08/03/2017.
 */
public interface UserService {

  User createUser(User user);
  User saveOrUpdate(User user);
  User findOneByEmail(String email);
  User findOne(Long userid);
  void delete(Long userid);
  User updateUser(User updateData);
}
