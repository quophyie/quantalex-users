package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
import com.quantal.exchange.users.dto.ApiGatewayUserResponseDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialRequestDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialsListResponseDto;
import com.quantal.javashared.constants.CommonConstants;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 19/04/2017.
 */
public interface ApiGatewayService {
    @POST("/consumers")
    CompletableFuture<ApiGatewayUserResponseDto> addUer(@Body ApiGatewayUserRequestDto user, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);


    @POST("/consumers/{consumer}/jwt")
    @Headers({"Content-Type: application/json"})
    //@Headers({"Content-Type: x-www-form-urlencoded"})
    //@FormUrlEncoded
    CompletableFuture<ApiJwtUserCredentialResponseDto> requestConsumerJwtCredentials(@Path("consumer") String apiUserId, @Body ApiJwtUserCredentialRequestDto requestDto, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @GET("/consumers/{consumer}/jwt")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<ApiJwtUserCredentialsListResponseDto> getConsumerJwtCredentials(@Path("consumer") String apiUserId, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);


    @DELETE("/consumers/{consumer}/jwt/{id}")
    CompletableFuture<Object> deleteConsumerJwtCredentials(@Path("consumer") String apiUserId, @Path("id")String credentialId, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);




}