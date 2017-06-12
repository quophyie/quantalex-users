package com.quantal.exchange.users.controllers;

import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.LoginResponseDto;
import com.quantal.exchange.users.facades.LoginFacade;
import com.quantal.shared.util.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by root on 08/06/2017.
 */

@RunWith(SpringRunner.class)
@WebMvcTest(value = LoginController.class, secure = false)
public class LoginControllerTests {

    private LoginController loginController;

    @MockBean
    private LoginFacade loginFacade;

    @Autowired
    private MockMvc mvc;


    @Before
    public void setUp() {
        loginController = new LoginController(loginFacade, null);
    }

    @Test
    public void shouldReturn200GivenValidEmailAndPassword () throws Exception {

        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("user@quantal.com");
        loginDto.setPassword("password");
        String jwt = "jwt_token";

        given(loginFacade.login(loginDto)).willAnswer(invocationOnMock -> {
            LoginResponseDto loginResponseDto = new LoginResponseDto();
            loginResponseDto.setToken(jwt);
            ResponseEntity<?> responseEntity = new ResponseEntity<>(loginResponseDto,HttpStatus.OK);
            return CompletableFuture.completedFuture(responseEntity);
        });

        MvcResult asyscResult = this.mvc.perform(post("/login/")
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


        MvcResult asyscResult = this.mvc.perform(post("/logout/{userId}", userId )
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", authHeader)
                .content(TestUtil.convertObjectToJsonString(loginDto)))
                .andReturn();

        mvc.perform(asyncDispatch(asyscResult))
                .andExpect(status().isOk());
    }
}
