package com.quantal.exchange.users.dto;

import com.quantal.exchange.users.enums.EmailType;
import lombok.Data;

/**
 * Created by root on 12/06/2017.
 */

@Data
public class EmailRequestDto {

    private String to;
    private EmailType emailType;
    private String  token;
    private String templateName;
}
