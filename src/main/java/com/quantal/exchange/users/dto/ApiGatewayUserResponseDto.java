package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by dman on 20/04/2017.
 */
@Data
public class ApiGatewayUserResponseDto extends ApiGatewayUserRequestDto {

    private String id;

    @JsonIgnore
    private LocalDate created_at;


}
