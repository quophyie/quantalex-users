package com.quantal.exchange.users.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Created by dman on 09/10/2017.
 */
@Data
public class LogLine {
    private String event;
    private String msg;
    private String proglang;
    private String framework;
    private String frameworkVersion;
    private String name;
    private String hostname;
    private String moduleVersion;
    private String lang;
    private LocalDate time;
}
