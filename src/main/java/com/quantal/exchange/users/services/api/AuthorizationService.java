package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.AuthResponseDto;
import com.quantal.exchange.users.dto.TokenDto;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 11/06/2017.
 */
public interface AuthorizationService {

    @DELETE("/{jti}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<AuthResponseDto> deleteToken(String jti);

    @POST("/{jti}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<TokenDto> requestToken(@Body AuthRequestDto authRequestDto);

}
