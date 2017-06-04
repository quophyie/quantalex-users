package com.quantal.exchange.users.dto;

import lombok.Data;

/**
 * Created by dman on 03/05/2017.
 */
@Data
public class ApiJwtUserCredentialResponseDto {

    private String consumer_id;

    private String key;
    private String algorithm;
    private String secret;

}
