package com.quantal.exchange.users.dto;

import com.quantal.exchange.users.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by root on 12/06/2017.
 */

@Data
@Builder
@AllArgsConstructor
public class EmailRequestDto {

    public EmailRequestDto(){}
    private String to;
    private EmailType emailType;
    private String  token;
    private String templateName;
}
