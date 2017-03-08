package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.enums.UserStatus;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by dman on 08/03/2017.
 */
@Data
public class UserDto {
  private Long id;

  private String email;
  private String password;
  private String firstName;
  private String lastName;
 // @JsonDeserialize(using = LocalDateDeserializer.class)
 // @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate dob;
  private LocalDate joinDate;
  private LocalDate activeDate;
  private LocalDate deactivatedDate;
  private Long companyId;
  private Gender gender;
  private UserStatus status;

}
