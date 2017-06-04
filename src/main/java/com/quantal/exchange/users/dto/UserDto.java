package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.annotation.JsonView;
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
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private Long id;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String email;
  private String password;

  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String firstName;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String lastName;
 // @JsonDeserialize(using = LocalDateDeserializer.class)
 // @JsonSerialize(using = LocalDateSerializer.class)
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private LocalDate dob;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private LocalDate joinDate;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private LocalDate activeDate;
  private LocalDate deactivatedDate;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private Long companyId;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private Gender gender;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private UserStatus status;

  private String apiUserId;

  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String token;



}
