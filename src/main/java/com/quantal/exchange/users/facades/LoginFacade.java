package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.LoginResponseDto;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.shared.facades.AbstractBaseFacade;
import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.shared.objectmapper.OrikaBeanMapper;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.shared.util.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by root on 08/06/2017.
 */
@Component
public class LoginFacade extends AbstractBaseFacade {

    private Logger logger  = LogManager.getLogger();
    private MessageService messageService;
    private LoginService loginService;



    @Autowired
    public LoginFacade(OrikaBeanMapper orikaBeanMapper,
                       NullSkippingOrikaBeanMapper nullSkippingMapper,
                       MessageService messageService,
                       LoginService loginService){
        super(orikaBeanMapper, nullSkippingMapper);
        this.messageService = messageService;
        this.loginService = loginService;
    }

    public CompletableFuture<ResponseEntity> login(LoginDto loginDto){
        String email = loginDto != null ? loginDto.getEmail() : "";
        logger.debug("Logging in user with email: {}", email);
        if (loginDto == null) {
            String msg = messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED);
            ResponseEntity<?> responseEntity = toRESTResponse(null, msg, HttpStatus.BAD_REQUEST);
            logger.debug(String.format("%s:%s",HttpStatus.BAD_REQUEST.toString(),msg));
            return CompletableFuture.completedFuture(responseEntity);
        }

        return loginService.login(loginDto.getEmail(), loginDto.getPassword())
                .thenApply(token -> {
                    LoginResponseDto loginResponseDto = new LoginResponseDto();
                    loginResponseDto.setToken(token);
                    logger.debug("login successful. token: {}", token);
                    ResponseEntity responseEntity = toRESTResponse(loginResponseDto, "");
                    return responseEntity;
                })
                .exceptionally(ex -> {
                    RuntimeException busEx = CommonUtils.extractBusinessException(ex);
                    ResponseEntity responseEntity =  toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                    if (busEx instanceof NotFoundException || busEx instanceof PasswordValidationException ) {
                        String errMsg = messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
                        responseEntity = toRESTResponse(null, errMsg, HttpStatus.UNAUTHORIZED);
                        logger.debug("login failed: " + errMsg+". {} . HttpStatus Code: {}", busEx, HttpStatus.UNAUTHORIZED);
                        return responseEntity;
                    }
                    return responseEntity;
                });
    }

    public CompletableFuture<ResponseEntity> logout(Long userId, String authHeader) {
        logger.debug("logging out user with Id {}", userId);

        if (StringUtils.isEmpty(authHeader)) {
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"authorisation header"});
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);
            logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
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
            logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
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

                    logger.debug("login failed: {}. HttpStatus Code: {}", message, HttpStatus.BAD_REQUEST.value());
                    return responseEntity;
                });
    }
}
