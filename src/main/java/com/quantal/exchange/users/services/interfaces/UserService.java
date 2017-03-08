package com.quantal.exchange.users.services.interfaces;

import com.quantal.exchange.users.models.User;

/**
 * Created by dman on 08/03/2017.
 */
public interface UserService {

  User saveOrUpdate(User user);

}
