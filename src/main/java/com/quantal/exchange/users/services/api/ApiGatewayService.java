package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
import com.quantal.exchange.users.dto.ApiGatewayUserResponseDto;
import retrofit2.http.*;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 19/04/2017.
 */
public interface ApiGatewayService {
    @POST("/consumers")
    CompletableFuture<ApiGatewayUserResponseDto> addUer(@Body ApiGatewayUserRequestDto user);
}