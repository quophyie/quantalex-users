package com.quantal.exchange.users.util;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.dto.ResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.Charset;

public class TestUtil {

  public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

  public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsBytes(object);
  }

  public static String convertObjectToJsonString(Object object) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writeValueAsString(object);
  }

  public static String createStringWithLength(int length) {
    StringBuilder builder = new StringBuilder();

    for (int index = 0; index < length; index++) {
      builder.append("a");
    }

    return builder.toString();
  }

  /**
   * Returns the data object of the ResponseDto in the response supplied ResponseEntity
   * @param responseEntity
   * @param <ModelT>
   * @return <ModelT> - the data object of the ResponseDto in the response supplied ResponseEntity
   */
  public static <ModelT> ModelT getResponseDtoData(ResponseEntity<?> responseEntity) {
    if (responseEntity!= null && !(responseEntity.getBody() instanceof ResponseDto) ){
      throw new IllegalArgumentException("Argument responseEntity must be instanceof ResponseEntity<ResponseDto>");
    }
     return  ((ResponseDto<ModelT>)responseEntity.getBody()).getData();
  }

  /**
   * Returns the message of the ResponseDto in the response supplied ResponseEntity
   * @param responseEntity
   * @return <ModelT> - the message of the ResponseDto in the response supplied ResponseEntity
   */
  public static  String getResponseDtoMessage(ResponseEntity<?> responseEntity) {
    if (responseEntity!= null && !(responseEntity.getBody() instanceof ResponseDto) ){
      throw new IllegalArgumentException("Argument responseEntity must be instanceof ResponseEntity<ResponseDto>");
    }
    return  ((ResponseDto)responseEntity.getBody()).getMessage();
  }
}