package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.constants.Events;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.LoginDto;
import com.quantal.exchange.users.dto.TokenDto;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.javashared.annotations.logger.InjectLogger;
import com.quantal.javashared.facades.AbstractBaseFacade;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.javashared.objectmapper.OrikaBeanMapper;
import com.quantal.javashared.services.interfaces.MessageService;
import com.quantal.javashared.util.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.spi.MDCAdapter;
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
    public CompletableFuture<ResponseEntity> login(LoginDto loginDto, MDCAdapter mdcAdapter){
        String email = loginDto != null ? loginDto.getEmail() : "";

        logger.with(EMAIL_KEY, email)
              .with(EVENT_KEY, Events.USER_LOGIN)
              .debug(String.format("Logging in user with username / email: %s", email));

        if (loginDto == null) {
            String msg = messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED);
            ResponseEntity<?> responseEntity = toRESTResponse(null, msg, HttpStatus.BAD_REQUEST);

            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.toString())
                  .with(com.quantal.javashared.constants.CommonConstants.EVENT_KEY, Events.USER_LOGIN)
                  .debug(String.format("%s",msg));
            return CompletableFuture.completedFuture(responseEntity);
        }

        return loginService.login(loginDto.getEmail(), loginDto.getPassword(), mdcAdapter)
                .thenApply(token -> {
                    TokenDto tokenDto = new TokenDto();
                    tokenDto.setToken(token);

                    logger.with(EVENT_KEY, Events.USER_LOGIN)
                          .with("token", token)
                          .info(String.format("login successful."));

                    ResponseEntity responseEntity = toRESTResponse(tokenDto, "");
                    return responseEntity;
                })
                .exceptionally(ex -> {
                    RuntimeException busEx = CommonUtils.extractBusinessExceptionAsRuntimeException(ex);
                    ResponseEntity responseEntity =  toRESTResponse(null, messageService.getMessage(MessageCodes.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
                    if (busEx instanceof NotFoundException || busEx instanceof PasswordValidationException ) {
                        String errMsg = messageService.getMessage(MessageCodes.INVALID_EMAIL_OR_PASSWORD);
                        responseEntity = toRESTResponse(null, errMsg, HttpStatus.UNAUTHORIZED);
                        logger.with(STATUS_CODE_KEY,  HttpStatus.UNAUTHORIZED)
                              .error(String.format("login failed:%s", errMsg), busEx);

                        return responseEntity;
                    }
                    logger.error("", busEx);
                    return responseEntity;
                });
    }

    public CompletableFuture<ResponseEntity> logout(Long userId, String authHeader) {

        logger.with(USER_KEY, userId)
              .with(EVENT_KEY, Events.USER_LOGOUT)
              .debug(String.format("logging out user with Id %s", userId));

        if (StringUtils.isEmpty(authHeader)) {
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"authorisation header"});
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);

            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value())
                  .with(EVENT_KEY, Events.USER_LOGOUT)
                  .info(String.format("login failed: %s.", message));

            return CompletableFuture.completedFuture(responseEntity);
        }

        String token = Pattern.compile(" ")
                .splitAsStream(authHeader)
                .filter(s -> !s.equalsIgnoreCase("bearer"))
                .collect(Collectors.joining(""));

        if (StringUtils.isEmpty(token)) {
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"authorization header bearer token"});
            ResponseEntity responseEntity = toRESTResponse(null, message, HttpStatus.BAD_REQUEST);

            logger.with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value())
                  .with(EVENT_KEY, Events.USER_LOGOUT)
                  .debug(String.format("login failed: %s", message));

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

                    logger.with(EVENT_KEY, ex)
                          .with(STATUS_CODE_KEY, HttpStatus.BAD_REQUEST.value()).debug(String.format("login failed: %s",message));

                    return responseEntity;
                });
    }
}
