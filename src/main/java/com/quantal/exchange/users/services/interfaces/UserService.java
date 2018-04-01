package com.quantal.exchange.users.services.interfaces;

import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.models.User;
import com.quantal.javashared.services.interfaces.RepositoryServiceAsync;
import org.slf4j.spi.MDCAdapter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 08/03/2017.
 */
public interface UserService extends RepositoryServiceAsync<User, Long> {

  CompletableFuture<User> createUser(User user, MDCAdapter mdcAdapter);
  CompletableFuture<User> saveOrUpdate(User user);
  CompletableFuture<User> findOneByEmail(String email, MDCAdapter mdcAdapter);
  //CompletableFuture<User> findOne(Long userid);
  CompletableFuture<Void> deleteById(Long userid);
  CompletableFuture  updateUser(User updateData) ;
  CompletableFuture<Long> countByEmailIgnoreCase(String email);
  CompletableFuture<List<User>> findAllByEmailIgnoreCase(String email);
  CompletableFuture<ApiJwtUserCredentialResponseDto> requestApiGatewayUserCredentials(String username);
  String createJwt(String issuer);
}
