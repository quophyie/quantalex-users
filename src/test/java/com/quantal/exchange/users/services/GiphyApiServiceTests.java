package com.quantal.exchange.users.services;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.*;

import com.quantal.exchange.users.services.api.GiphyApiService;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


import java.util.concurrent.ExecutionException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static net.javacrumbs.jsonunit.JsonMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by dman on 22/03/2017.
 */


@RunWith(MockitoJUnitRunner.class)
public class GiphyApiServiceTests {

    @ClassRule
     public static HoverflyRule hoverflyRule = HoverflyRule.inCaptureOrSimulationMode("simulation.json");

    private Retrofit retrofit;
    private GiphyApiService giphyApiService;

    @Before
    public void setUp() {
        retrofit =  new Retrofit.Builder()
                .baseUrl("https://testapi")
                .addConverterFactory(ScalarsConverterFactory.create())
                //.addConverterFactory(new StringConverterFactory())
                //.addConverterFactory(JacksonConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(Java8CallAdapterFactory.create())
                //.client(client)
                .build();
        giphyApiService = retrofit.create(GiphyApiService.class);
    }

    @Test
    public void shouldBeAbleToGetPokenHoverfly() throws ExecutionException, InterruptedException {

        // When
        String result = giphyApiService.getPokemon(20, 0).get();
        String jsonStringResult = StringEscapeUtils.unescapeJson(result);

        //Then
        assertThatJson(jsonStringResult)
                .node("count").isEqualTo(251)
                .node("results")
                .matches(hasItem(jsonPartMatches("name", equalTo("drizzle"))));
    }
}
