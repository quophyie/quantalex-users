package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.*;
import retrofit2.http.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 19/04/2017.
 */
public interface ApiGatewayService {
    @POST("/consumers")
    CompletableFuture<ApiGatewayUserResponseDto> addUer(@Body ApiGatewayUserRequestDto user);


    @POST("/consumers/{consumer}/jwt")
    @Headers({"Content-Type: application/json"})
    //@Headers({"Content-Type: x-www-form-urlencoded"})
    //@FormUrlEncoded
    CompletableFuture<ApiJwtUserCredentialResponseDto> requestConsumerJwtCredentials(@Path("consumer") String apiUserId, @Body ApiJwtUserCredentialRequestDto requestDto);

    @GET("/consumers/{consumer}/jwt")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<ApiJwtUserCredentialsListResponseDto> getConsumerJwtCredentials(@Path("consumer") String apiUserId);


    @DELETE("/consumers/{consumer}/jwt/{id}")
    CompletableFuture<Object> deleteConsumerJwtCredentials(@Path("consumer") String apiUserId, @Path("id")String credentialId);




}