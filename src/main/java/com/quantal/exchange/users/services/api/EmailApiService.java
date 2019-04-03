package com.quantal.exchange.users.services.api;

import com.quantal.exchange.users.dto.EmailRequestDto;
import com.quantal.exchange.users.dto.EmailResponseDto;
import com.quantal.javashared.annotations.requestheaders.EnforceRequiredHeaders;
import com.quantal.javashared.constants.CommonConstants;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 12/06/2017.
 */
@EnforceRequiredHeaders
public interface EmailApiService {
    @POST("email")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<EmailResponseDto> sendEmail(@Body EmailRequestDto emailRequestDto);

    @POST("email/template/{templateName}")
    @Headers({"Content-Type: application/json"})
    CompletableFuture<EmailResponseDto> sendEmailByTemplate(@Path("templateName") String templateName, @Body EmailRequestDto emailRequestDto, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);
}
