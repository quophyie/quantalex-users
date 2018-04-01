package com.quantal.exchange.users.dto;

import com.fasterxml.jackson.annotation.JsonView;
import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.enums.UserStatusEnum;
import com.quantal.exchange.users.jsonviews.UserViews;
import com.quantal.exchange.users.validators.email.ValidEmail;
import com.quantal.exchange.users.validators.password.PasswordMatches;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by dman on 08/03/2017.
 */
@Data
@PasswordMatches
public class UserDto {
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private Long id;

  @ValidEmail
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String email;
  private String password;
  private String confirmedPassword;

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
  private GenderEnum gender;
  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private UserStatusEnum status;

  private String apiUserId;

  @JsonView(UserViews.CreatedAndUpdatedUserView.class)
  private String token;



}
