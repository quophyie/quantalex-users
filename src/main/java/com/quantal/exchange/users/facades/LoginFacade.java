package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.javashared.annotations.logger.InjectLogger;
import com.quantal.javashared.facades.AbstractBaseFacade;
//import com.quantal.javashared.logger.LogField;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
//import com.savoirtech.logging.slf4j.json.logger.Logger;
import org.apache.commons.lang3.StringUtils;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.quantal.javashared.constants.CommonConstants.EMAIL_KEY;
import static com.quantal.javashared.constants.CommonConstants.EVENT_KEY;
import static com.quantal.javashared.constants.CommonConstants.STATUS_CODE_KEY;
import static com.quantal.javashared.constants.CommonConstants.USER_KEY;

//import com.savoirtech.logging.slf4j.json.LoggerFactory;

/**
 * Created by root on 08/06/2017.
 */
@Component
public class LoginFacade extends AbstractBaseFacade {

    //private Logger logger  = LogManager.getLogger();
    //private XLogger logger  = LoggerFactory.getLogger(this.getClass());
    @InjectLogger
    private QuantalLogger logger;
    private MessageService messageService;
    private LoginService loginService;

    @Autowired
    private ExecutorService taskExecutor;



    @Autowired
    public LoginFacade(OrikaBeanMapper orikaBeanMapper,
                       NullSkippingOrikaBeanMapper nullSkippingMapper,
                       MessageService messageService,
                       LoginService loginService){
        super(orikaBeanMapper, nullSkippingMapper);
        this.messageService = messageService;
        this.loginService = loginService;
    }

    //@Async
    public CompletableFuture<ResponseEntity> login(LoginDto loginDto){
        String email = loginDto != null ? loginDto.getEmail() : "";
        //logger.debug(String.format("Logging in user with email: %s", email), new LogField("email", email));
        logger.with(EMAIL_KEY, "email")
              .with(EVENT_KEY, "LOGIN")
              .debug(String.format("Logging in user with email: %s", email));
        //logger.debug("Logging in user with email: {}", email);
        /*logger.debug()
                .message(String.format("Logging in user with email: %s", email))
                .log();*/
        if (loginDto == null) {
            String msg = messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED);
            ResponseEntity<?> responseEntity = toRESTResponse(null, msg, HttpStatus.BAD_REQUEST);
            //logger.debug(String.format("%s:%s",HttpStatus.BAD_REQUEST.toString(),msg));
            //logger.debug(String.format("%s",msg), new LogField("statusCode", HttpStatus.BAD_REQUEST.toString()));
            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.toString())
                    .with(com.quantal.javashared.constants.CommonConstants.EVENT_KEY, "LOGIN")
                    .debug(String.format("%s",msg));
            /*logger.debug()
                    .message(String.format("%s:%s",HttpStatus.BAD_REQUEST.toString(),msg))
                    .log();*/
            return CompletableFuture.completedFuture(responseEntity);
        }

        return loginService.login(loginDto.getEmail(), loginDto.getPassword())
                .thenApplyAsync(token -> {
                    TokenDto tokenDto = new TokenDto();
                    tokenDto.setToken(token);
                    //logger.info("login successful. token: {}", token);
                    logger.with(EVENT_KEY, "LOGIN")
                            .with("token", token)
                            .info(String.format("login successful."));
                    /*logger.info()
                            .message(String.format("login successful. token: %s", token))
                            .log();*/
                    ResponseEntity responseEntity = toRESTResponse(tokenDto, "");
                    return responseEntity;
                }, taskExecutor)
                .exceptionally(ex -> {
                    RuntimeException busEx = CommonUtils.extractBusinessExceptionAsRuntimeException(ex);
                    ResponseEntity responseEntity =  toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                    if (busEx instanceof NotFoundException || busEx instanceof PasswordValidationException ) {
                        String errMsg = messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
                        responseEntity = toRESTResponse(null, errMsg, HttpStatus.UNAUTHORIZED);
                        //logger.info("login failed: " + errMsg+". {} . HttpStatus Code: {}", busEx, HttpStatus.UNAUTHORIZED);
                        logger
                                .with(STATUS_CODE_KEY,  HttpStatus.UNAUTHORIZED)
                                .error(String.format("login failed:%s", errMsg), busEx);
                        /*logger.error()
                                .exception(String.format("login failed: %s. HttpStatus Code: {}",errMsg, HttpStatus.UNAUTHORIZED),busEx)
                                .log();*/
                        return responseEntity;
                    }
                    //logger.error(busEx);
                    //logger.error().exception("", busEx);
                    logger.error("", busEx);
                    return responseEntity;
                });
    }

    public CompletableFuture<ResponseEntity> logout(Long userId, String authHeader) {
        //logger.debug("logging out user with Id {}", userId);
        logger.with(USER_KEY, userId)
                .with(EVENT_KEY, "LOGOUT")
                .debug(String.format("logging out user with Id %s", userId));
       /* logger.debug()
        .message(String.format("logging out user with Id %d", userId))
        .field("userId", userId)
        .log();*/

        if (StringUtils.isEmpty(authHeader)) {
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"authorisation header"});
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
            //logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value())
                    .with(EVENT_KEY, "LOGOUT")
                    .info(String.format("login failed: %s.", message));
            /*logger.debug()
                    .message(String.format("login failed: %s.", message))
                    .field("statusCode",HttpStatus.BAD_REQUEST.value())
                    .log();*/
            return CompletableFuture.completedFuture(responseEntity);
        }

        //Extract the jwt
        String token = Pattern.compile(" ")
                .splitAsStream(authHeader)
                .filter(s -> !s.equalsIgnoreCase("bearer"))
                .collect(Collectors.joining(""));

        if (StringUtils.isEmpty(token)) {
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"authorization header bearer token"});
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
            //logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
           // logger.debug(String.format("login failed: %s", message), new LogField("statusField", HttpStatus.BAD_REQUEST.value()));
            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value())
                    .with(EVENT_KEY, "LOGOUT")
                    .debug(String.format("login failed: %s", message));
            /*logger.debug()
            .message(String.format("login failed: %s", message))
            .field("statusCode", HttpStatus.BAD_REQUEST.value())
            .log();*/

            return CompletableFuture.completedFuture(responseEntity);
        }

        return loginService
                .logout(token)
                .thenApply(v -> {
                    String message = messageService.getMessage(MessageCodes.SUCCESS);
                    ResponseEntity responseEntity = toRESTResponse(null, message);
                    return responseEntity;
                })
                .exceptionally(ex -> {
                    String message = messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR);
                    ResponseEntity responseEntity = toRESTResponse(null, message);
                    RuntimeException busEx = CommonUtils.extractBusinessException(ex);

                    if (busEx instanceof IllegalArgumentException) {
                        message = busEx.getMessage();
                        responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
                    }

                    //logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
                    //logger.debug(String.format("login failed: %s",message), new LogField("statusField", HttpStatus.BAD_REQUEST.value()));
                    logger
                            .with(EVENT_KEY, ex)
                            .with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value()).debug(String.format("login failed: %s",message));

                    /* logger.debug()
                    .message(String.format("login failed: %s", message))
                    .field("statusCode",HttpStatus.BAD_REQUEST.value())
                    .log();*/
                    return responseEntity;
                });
    }
}
