package com.quantal.exchange.users.models;

import com.quantal.exchange.users.enums.UserStatusEnum;
import com.quantal.javashared.convertors.jpa.LocalDateJpaConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
  @Column(name = "first_name")
  private String firstName;
  @Column(name = "last_name")
  private String lastName;
  @Convert(converter = LocalDateJpaConverter.class)
  private LocalDate dob;
  @JoinColumn(name = "name")
  @ManyToOne
  private Gender gender;

  @Convert(converter = LocalDateJpaConverter.class)
  @Column(name = "join_date")
  private LocalDate joinDate;

  @Convert(converter = LocalDateJpaConverter.class)
  @Column(name = "active_date")
  private LocalDate activeDate;

  @Convert(converter = LocalDateJpaConverter.class)
  @Column(name = "deactived_date")
  private LocalDate deactivatedDate;

  @Column(name = "company_id")
  private Long companyId;
  @JoinColumn(name = "status")
  @ManyToOne
  private UserStatus status;
  private String apiUserId;
}
