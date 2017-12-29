package com.quantal.exchange.users.services.implementations;

import com.godaddy.logging.Logger;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.AuthRequestDto;
import com.quantal.exchange.users.exceptions.InvalidDataException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.api.AuthorizationApiService;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.javashared.dto.CommonLogFields;
import com.quantal.javashared.dto.LogEvent;
import com.quantal.javashared.dto.LogzioConfig;
import com.quantal.javashared.logger.LogField;
import com.quantal.javashared.logger.LoggerFactory;
import com.quantal.javashared.logger.QuantalLogger;
import com.quantal.javashared.logger.QuantalLoggerFactory;
import com.quantal.javashared.services.interfaces.MessageService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.slf4j.ext.XLogger;
//import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Created by root on 09/06/2017.
 */

@Service
public class LoginServiceImpl implements LoginService {

    private UserService userService;
    private PasswordService passwordService;
    private MessageService messageService;
    private ApiGatewayService apiGatewayService;
    private AuthorizationApiService authorizationApiService;


    @Autowired
    private ExecutorService taskExecutor;
    //private static Logger logger = LogManager.getLogger();
   // private final XLogger logger = XLoggerFactory.getXLogger(this.getClass().getName());
   //private final XLogger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final QuantalLogger logger;

    @Value("#{environment.JWT_SECRET}")
    private String JWT_SECRET;

    @Autowired
    public LoginServiceImpl(UserService userService,
                            PasswordService passwordService,
                            MessageService messageService,
                            ApiGatewayService apiGatewayService,
                            AuthorizationApiService authorizationApiService,
                            CommonLogFields commonLogFields,
                            LogzioConfig logzioConfig) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.messageService = messageService;
        this.apiGatewayService  = apiGatewayService;
        this.authorizationApiService = authorizationApiService;
        logger = QuantalLoggerFactory.getLogzioLogger(this.getClass(), commonLogFields, logzioConfig);
    }

    @Override
    //@Async
    public CompletableFuture<String> login(String email, String password) {
        //logger.debug("Logging in user with email: {}", email);
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
       // logger.debug(String.format("Logging in user with email: {}", email), new LogField("email", email));
        logger.with("email", email).info(String.format("Logging in user"), new LogEvent("LOGGING_IN"));
       return userService.findOneByEmail(email)
                .thenApplyAsync(user -> {
                    if (user == null) {
                        String message = String.format(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
                        //logger.debug(message);
                         throw logger.throwing(new NotFoundException(message));
                    }
                    //logger.info("found user identified by {}",email );
                    logger.info(String.format("found user identified by %s",email), new LogField("email", email), new LogField("user", user), new LogEvent("LOGGING_IN"));
                    return user;
                }, taskExecutor)
               .thenApplyAsync(user -> {
                   if (!passwordService.checkPassword(password, user.getPassword())) {
                       throw logger.throwing(new PasswordValidationException(""));
                   }
                   //logger.debug("Requesting login token for {} ... ", email);
                   logger.debug(String.format("Requesting login token for %s ... ", email), new LogField("email", email), new LogEvent("LOGGING_IN"));
                   AuthRequestDto authRequestDto = new AuthRequestDto();
                   authRequestDto.setEmail(email);
                   return authorizationApiService.requestToken(authRequestDto);
               }, taskExecutor)
              // .handle((apiJwtUserResponseCompletableFuture, ex) -> CommonUtils.processHandle(apiJwtUserResponseCompletableFuture, ex))
               .thenCompose(tokenCompletableFuture -> tokenCompletableFuture)
               .thenApply(token -> token.getToken());


    }

    @Override
    public CompletableFuture<Void> logout(String jwt)  {

        try {
            String message = "";

            if (StringUtils.isEmpty(jwt)) {
                message = messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED);
                throw logger.throwing(new IllegalArgumentException(message));
            }

            String keyAsBase64 = Base64.getEncoder().encodeToString(JWT_SECRET.getBytes("utf-8"));
            String jti = Jwts
                    .parser()
                    .setSigningKey(keyAsBase64)
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getId();

            if (StringUtils.isEmpty(jti)) {
                message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[]{"jti"});
                throw logger.throwing(new IllegalArgumentException(message));
            }

            //logger.debug("Contacting authorization service to delete token with jti {} ...", jti);
            logger.debug(String.format("Contacting authorization service to delete token with jti %s ...", jti), new LogField("jti", jti));
            return authorizationApiService
                    .deleteToken(jti)
                    .thenApply(authResponseDto -> null);

        } catch (SignatureException | UnsupportedEncodingException se) {

            try {
                throw logger.throwing(se);
            } catch (Exception e) {
                throw logger.throwing(new RuntimeException(e));
            }
        }
    }
}

