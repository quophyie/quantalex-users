package com.quantal.exchange.users.dto;

import lombok.Data;

import java.util.List;

/**
 * Created by dman on 03/05/2017.
 */
@Data
public class ApiJwtUserCredentialsListResponseDto {

    List<ApiJwtUserCredentialResponseDto> data;
    int total;

}
