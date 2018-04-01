package com.quantal.exchange.users.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "user_statuses")
@Entity
@Data
public class UserStatus {

    @Id
    private String status;
    private String description;


}
