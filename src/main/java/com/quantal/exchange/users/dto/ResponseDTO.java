package com.quantal.exchange.users.dto;

import lombok.Data;

/**
 * Created by dman on 08/03/2017.
 */

@Data
public class ResponseDTO<TData> extends ResponseMessageDTO {

  private TData data;

  public ResponseDTO(String message, int code, TData data){
    super(message, code);
    this.data = data;
  }

  public ResponseDTO(TData data) {
    this.data = data;
  }

  public TData getData() {
    return data;
  }

  public void setData(TData data) {
    this.data = data;
  }
}

