package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.dto.AuthResponseDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.javashared.annotations.requestheaders.EnforceRequiredHeaders;
import com.quantal.javashared.constants.CommonConstants;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 11/06/2017.
 */
@EnforceRequiredHeaders
public interface AuthorizationApiService  {

    @DELETE("user/token/one/{jti}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<AuthResponseDto> deleteToken(String jti, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @POST("user/token")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<TokenDto> requestToken(@Body AuthRequestDto requestDto, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @DELETE("user/token/{email}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<AuthResponseDto> deleteAllTokens(String userEmail, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @POST("user/token/verify")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<ApiJwtUserCredentialResponseDto> verifyToken(@Body TokenDto tokenDto, @   Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @POST("user/credential/")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<Void> requestUserCredentials(@Body AuthRequestDto requestDto, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);


    @DELETE("user/credential/{email}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<AuthResponseDto> deleteUserCredentials(@Path("email") String userEmail, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);
}
