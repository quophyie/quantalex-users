package com.quantal.exchange.users.controllers;

import com.quantal.exchange.users.controlleradvice.ExceptionHandlerControllerAdvice;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.facades.LoginFacade;
import com.quantal.exchange.users.facades.UserManagementFacade;
import com.quantal.javashared.constants.CommonConstants;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LoggerConfig;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.spi.MDCAdapter;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.CompletableFuture;

import static com.quantal.exchange.users.constants.TestConstants.EVENT;
import static com.quantal.exchange.users.constants.TestConstants.TRACE_ID;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by root on 08/06/2017.
 */

@RunWith(SpringRunner.class)
//@WebMvcTest(value = LoginController.class, secure = false)
//@SpringBootTest(classes = {UsersApplication.class})
//@ContextConfiguration(classes = { WebStartupConfig.class })
//@WebAppConfiguration
public class LoginControllerTests {

    private LoginController loginController;

    @MockBean
    private LoginFacade loginFacade;

   // @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageService messageService;

    @Mock
    private MDCAdapter mdcAdapter;

    @Before
    public void setUp() {

        given(mdcAdapter.get(CommonConstants.TRACE_ID_MDC_KEY)).willReturn(TRACE_ID);
        given(mdcAdapter.get(CommonConstants.EVENT_KEY)).willReturn(EVENT);
        QuantalLogger quantalLogger = QuantalLoggerFactory.getLogger(UserManagementFacade.class,  LoggerConfig.builder().commonLogFields(new CommonLogFields()).build());
        quantalLogger = (QuantalLogger) quantalLogger.with(CommonConstants.TRACE_ID_MDC_KEY, TRACE_ID).with(CommonConstants.EVENT_KEY, "EVENT");


        loginController = new LoginController(loginFacade, null);
        ReflectionTestUtils.setField(loginController, "logger", quantalLogger);
       mvc=  MockMvcBuilders.standaloneSetup(loginController)
                .setControllerAdvice(new ExceptionHandlerControllerAdvice(messageService)).build();

    }

    @Test
    public void shouldReturn200GivenValidEmailAndPassword () throws Exception {

        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("user@quantal.com");
        loginDto.setPassword("password");
        String jwt = "jwt_token";

        given(loginFacade.login(eq(loginDto), any(MDCAdapter.class))).willAnswer(invocationOnMock -> {
            TokenDto tokenDto = new TokenDto();
            tokenDto.setToken(jwt);
            ResponseEntity<?> responseEntity = new ResponseEntity<>(tokenDto,HttpStatus.OK);
            return CompletableFuture.completedFuture(responseEntity);
        });

        MvcResult asyscResult = this.mvc.perform(post("/v1/login")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(TestUtil.convertObjectToJsonString(loginDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))

                .andExpect(status().isOk())
                .andExpect(
                        json()
                                .isObject())
                .andExpect(
                        json()
                                .node("token")
                                .isEqualTo(jwt));
    }

    @Test
    public void shouldLogoutUserAndReturn200GivenLogoutDetails() throws Exception {

        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("user@quantal.com");
        String jwt = "jwt_token";
        String authHeader = "Bearer "+jwt;
        Long userId = 1L;

        given(loginFacade.logout(userId, authHeader)).willAnswer(invocationOnMock -> {
            ResponseEntity<?> responseEntity = new ResponseEntity<>(null,HttpStatus.OK);
            return CompletableFuture.completedFuture(responseEntity);
        });


        MvcResult asyscResult = this.mvc.perform(post("/v1/logout/{userId}", userId )
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", authHeader)
                .content(TestUtil.convertObjectToJsonString(loginDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isOk());
    }
}
