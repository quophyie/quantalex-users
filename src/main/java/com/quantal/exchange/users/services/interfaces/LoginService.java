package com.quantal.exchange.users.services.interfaces;

import com.quantal.exchange.users.dto.LoginDto;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 09/06/2017.
 */
public interface LoginService {

    CompletableFuture<String> login(String email, String password);
    CompletableFuture<Void> logout(String jwt) ;
}
