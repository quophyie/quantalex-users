package com.quantal.exchange.users.dto;

import com.quantal.exchange.users.enums.TokenType;
import lombok.Data;

/**
 * Created by root on 12/06/2017.
 */
@Data
public class AuthRequestDto {

    private String email;
    private TokenType tokenType;
}
