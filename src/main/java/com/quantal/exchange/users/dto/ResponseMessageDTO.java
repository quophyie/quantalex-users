package com.quantal.exchange.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by dman on 08/03/2017.
 */
@Data
public class ResponseMessageDTO {
  private String message;
  private int code;

  public ResponseMessageDTO(){}
  public ResponseMessageDTO(String message, int code) {
    this.code = code;
    this.message = message;

  }
}

