package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.quantal.exchange.users.jsonviews.LoginView;
import lombok.Data;

/**
 * Created by root on 08/06/2017.
 */

@Data
public class TokenDto {

    @JsonView(LoginView.LoginResponse.class)
    private String token;
}
