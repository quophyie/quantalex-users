package com.quantal.exchange.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by root on 12/06/2017.
 */

@Builder
@Data
@AllArgsConstructor
public class EmailResponseDto {
    public EmailResponseDto(){}
    private int code;
}
