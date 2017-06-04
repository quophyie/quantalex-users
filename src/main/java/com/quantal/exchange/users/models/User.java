package com.quantal.exchange.users.models;

import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Created by dman on 07/03/2017.
 */

@Entity
@Table(name = "users")
@Data
@ToString
@EqualsAndHashCode (of = {"id"})
public class User implements Serializable{

  @Id
  @GeneratedValue
  private Long id;

  private String email;
  private String password;
  private String firstName;
  private String lastName;
  private LocalDate dob;
  private Gender gender;
  private LocalDate joinDate;
  private LocalDate activeDate;
  private LocalDate deactivatedDate;
  private Long companyId;
  private UserStatus status;
  private String apiUserId;
}
