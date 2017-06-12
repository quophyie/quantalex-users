package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.AuthResponseDto;
import retrofit2.http.DELETE;
import retrofit2.http.Headers;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 11/06/2017.
 */
public interface AuthorizationService {

    @DELETE("/{jti}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<AuthResponseDto> deleteToken(String jti);

}
