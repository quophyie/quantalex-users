package com.quantal.exchange.users.services.api;

import com.quantal.javashared.constants.CommonConstants;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

import java.util.concurrent.CompletableFuture;

/**
 * Created by dman on 12/03/2017.
 */


public interface GiphyApiService {


    // http://api.giphy.com/v1/gifs/search?q=funny+cat&api_key=dc6zaTOxFJmzC
    /*public GiphyApiService(String baseUrl) {
        super(baseUrl);
    }*/

    @GET("http://api.giphy.com/v1/gifs/search")
    CompletableFuture<String> getGiphy(@Query("q") String query, @Query("api_key") String apiKey, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);

    @GET("http://pokeapi.co/api/v2/ability/ ")
    CompletableFuture<String> getPokemon(@Query("limit") int limit, @Query("offset") int offset, @Header(CommonConstants.EVENT_HEADER_KEY) String event, @Header(CommonConstants.TRACE_ID_HEADER_KEY) String traceId);
}
