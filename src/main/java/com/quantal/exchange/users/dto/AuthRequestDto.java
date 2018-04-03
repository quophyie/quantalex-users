package com.quantal.exchange.users.dto;

import com.quantal.exchange.users.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by root on 12/06/2017.
 */
@AllArgsConstructor
@Builder
@Data
public class AuthRequestDto {

    public AuthRequestDto(){}
    private String email;
    private TokenType tokenType;
}
