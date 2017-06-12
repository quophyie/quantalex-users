package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.dto.EmailResponseDto;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 12/06/2017.
 */
public interface EmailService {
    @POST("")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<EmailResponseDto> sendEmail(@Body EmailRequestDto emailRequestDto);
}
