package com.quantal.exchange.users.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name="gender")
@Entity
public class Gender {

    @Id
    //@GeneratedValue
    //private int id;

    private String name;
    private String description;
}
