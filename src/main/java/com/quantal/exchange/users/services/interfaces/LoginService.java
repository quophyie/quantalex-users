package com.quantal.exchange.users.services.interfaces;

import org.slf4j.spi.MDCAdapter;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 09/06/2017.
 */
public interface LoginService {

    CompletableFuture<String> login(String email, String password, MDCAdapter mdcAdapter);
    CompletableFuture<Void> logout(String jwt) ;
}
