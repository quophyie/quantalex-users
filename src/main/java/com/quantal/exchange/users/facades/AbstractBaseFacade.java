package com.quantal.exchange.users.facades;


import com.quantal.exchange.users.dto.ResponseDTO;
import com.quantal.exchange.users.objectmapper.OrikaBeanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


public abstract class AbstractBaseFacade {

  @Autowired
  protected OrikaBeanMapper mapper;


  public static <TResponseDTOData> ResponseEntity<?> toRESTResponse(TResponseDTOData reponseDTOData, HttpStatus httpStatus, HttpHeaders httpHeaders){

    ResponseEntity<ResponseDTO<TResponseDTOData>> response;
    ResponseDTO<TResponseDTOData> responseDTO = new ResponseDTO<>(reponseDTOData);
    if (httpHeaders != null){
      response = new ResponseEntity<>(responseDTO, httpHeaders, httpStatus);
    } else {
      response = new ResponseEntity<>(responseDTO, httpStatus);
    }
    return response;
  }

  public static <TResponseDTOData> ResponseEntity<?> toRESTResponse(TResponseDTOData reponseDTOData, HttpStatus httpStatus){
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE,  MediaType.APPLICATION_JSON_VALUE);
    return toRESTResponse(reponseDTOData,httpStatus,headers);
  }


  public static <TResponseDTOData> ResponseEntity<?> toRESTResponse(TResponseDTOData reponseDTOData){
    return toRESTResponse(reponseDTOData,HttpStatus.OK);
  }

  public  <TDTO, TModel> TModel toModel(TDTO source, Class<TModel> clazz){
    TModel model = mapper.map(source, clazz);
    return model;
  }

  public  <TModel, TDTO> TDTO toDto(TModel source, Class<TDTO> clazz){
    TDTO dto = mapper.map(source, clazz);
    return dto;
  }
}
