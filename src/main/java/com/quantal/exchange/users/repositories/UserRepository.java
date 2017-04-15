package com.quantal.exchange.users.repositories;

import com.quantal.exchange.users.models.User;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Created by dman on 08/03/2017.
 */
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
  User findOneByEmail(String email);
  Long countByEmailIgnoreCase(String email);
  List<User> findAllByEmailIgnoreCase(String email);
  //User findOneByUsername(String username);
}
