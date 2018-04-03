package com.quantal.exchange.users.services;

import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.javashared.constants.CommonConstants;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.springframework.test.context.junit4.SpringRunner;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.concurrent.ExecutionException;

import static com.quantal.exchange.users.constants.TestConstants.TRACE_ID;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.TRACE_ID_MDC_KEY;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by dman on 22/03/2017.
 */


@RunWith(SpringRunner.class)
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
        MDC.put(TRACE_ID_MDC_KEY, TRACE_ID);
        MDC.put(EVENT_KEY, TRACE_ID);
        giphyApiService = retrofit.create(GiphyApiService.class);
    }

    @Test
    public void shouldBeAbleToGetPokenHoverfly() throws ExecutionException, InterruptedException {

        // When

        String result = giphyApiService.getPokemon(20, 0, MDC.getMDCAdapter().get(CommonConstants.EVENT_KEY), MDC.getMDCAdapter().get(CommonConstants.TRACE_ID_MDC_KEY)).get();
        String jsonStringResult = StringEscapeUtils.unescapeJson(result);

        //Then
        assertThatJson(jsonStringResult)
                .node("count").isEqualTo(251)
                .node("results")
                .matches(hasItem(jsonPartMatches("name", equalTo("drizzle"))));
    }
}
