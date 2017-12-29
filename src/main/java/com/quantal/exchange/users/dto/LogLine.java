package com.quantal.exchange.users.dto;

import com.quantal.javashared.logger.LogField;
import lombok.Data;


/**
 * Created by dman on 09/10/2017.
 */
@Data
public class LogLine {
    private LogField event;
    private LogField msg;
    private LogField proglang;
    private LogField framework;
    private LogField frameworkVersion;
    private LogField name;
    private LogField hostname;
    private LogField moduleVersion;
    private LogField lang;
    private LogField time;
}
