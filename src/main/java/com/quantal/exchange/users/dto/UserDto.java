package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.enums.UserStatus;
import com.quantal.exchange.users.jsonviews.UserViews;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by dman on 08/03/2017.
 */
@Data
public class UserDto {
  @JsonView(UserViews.CreatedUserView.class)
  private Long id;
  @JsonView(UserViews.CreatedUserView.class)
  private String email;
  private String password;
  @JsonView(UserViews.CreatedUserView.class)
  private String firstName;
  @JsonView(UserViews.CreatedUserView.class)
  private String lastName;
 // @JsonDeserialize(using = LocalDateDeserializer.class)
 // @JsonSerialize(using = LocalDateSerializer.class)
  @JsonView(UserViews.CreatedUserView.class)
  private LocalDate dob;
  @JsonView(UserViews.CreatedUserView.class)
  private LocalDate joinDate;
  @JsonView(UserViews.CreatedUserView.class)
  private LocalDate activeDate;
  private LocalDate deactivatedDate;
  @JsonView(UserViews.CreatedUserView.class)
  private Long companyId;
  @JsonView(UserViews.CreatedUserView.class)
  private Gender gender;
  @JsonView(UserViews.CreatedUserView.class)
  private UserStatus status;

}
