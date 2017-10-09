package com.quantal.exchange.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.facades.LoginFacade;
import com.quantal.exchange.users.jsonviews.LoginView;
import com.quantal.shared.controller.BaseControllerAsync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 08/06/2017.
 */
@RestController
@RequestMapping("")
public class LoginController extends BaseControllerAsync{

    private LoginFacade loginFacade;
    private ObjectMapper objectMapper;

    @Autowired
    public LoginController(LoginFacade loginFacade,
                           ObjectMapper objectMapper){
        this.loginFacade = loginFacade;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/login/", consumes = MediaType.APPLICATION_JSON_VALUE)
    //@Async
    public CompletableFuture<ResponseEntity<?>> login(@RequestBody LoginDto loginDto){
        return loginFacade.login(loginDto)
                .thenApply(responseEntity -> applyJsonView(responseEntity, LoginView.LoginResponse.class, objectMapper));
    }

    @PostMapping(value = "/logout/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<?>> logout(@PathVariable Long userId, @RequestHeader("Authorization") String authHeader){
        return loginFacade.logout( userId, authHeader)
                .thenApply(responseEntity -> applyJsonView(responseEntity, LoginView.LoginResponse.class, objectMapper));
    }


}
