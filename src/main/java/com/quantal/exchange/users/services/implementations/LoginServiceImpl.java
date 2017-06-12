package com.quantal.exchange.users.services.implementations;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.dto.AuthResponseDto;
import com.quantal.exchange.users.exceptions.InvalidDataException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.api.AuthorizationService;
import com.quantal.exchange.users.services.interfaces.LoginService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.shared.util.CommonUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Created by root on 09/06/2017.
 */

@Service
public class LoginServiceImpl implements LoginService {

    private UserService userService;
    private PasswordService passwordService;
    private MessageService messageService;
    private ApiGatewayService apiGatewayService;
    private AuthorizationService authorizationService;

    private static Logger logger = LogManager.getLogger();

    @Value("#{environment.JWT_SECRET}")
    private String JWT_SECRET;

    @Autowired
    public LoginServiceImpl(UserService userService,
                            PasswordService passwordService,
                            MessageService messageService,
                            ApiGatewayService apiGatewayService,
                            AuthorizationService authorizationService) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.messageService = messageService;
        this.apiGatewayService  = apiGatewayService;
        this.authorizationService = authorizationService;
    }

    @Override
    public CompletableFuture<String> login(String email, String password) {
        logger.debug("Logging in user with email: {}", email);
       return userService.findOneByEmail(email)
                .thenApplyAsync(user -> {
                    if (user == null) {
                        String message = String.format(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
                        //logger.debug(message);
                        throw logger.throwing(new NotFoundException(message));
                    }
                    return user;
                })
               .thenApplyAsync(user -> {
                   if (!passwordService.checkPassword(password, user.getPassword())) {
                       throw logger.throwing(new PasswordValidationException(""));
                   }
                   logger.debug("Requesting API credentials for {} ... ", email);
                   return apiGatewayService.getConsumerJwtCredentials(user.getEmail());
               })
              // .handle((apiJwtUserResponseCompletableFuture, ex) -> CommonUtils.processHandle(apiJwtUserResponseCompletableFuture, ex))
               .thenCompose(apiJwtUserCredentialResponseDto -> {
                   logger.debug("API credentials for {} found ", email);
                   return apiJwtUserCredentialResponseDto;
               })
               .thenApply(apiJwtUserResponse -> {

                   if (apiJwtUserResponse.getData().isEmpty()) {
                       String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[] {"api credential key "});
                       throw logger.throwing(new InvalidDataException(message));
                   }
                   return userService.createJwt(apiJwtUserResponse.getData().get(0).getKey());
               });

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

            logger.debug("Contacting authorization service to delete token with jti {} ...", jti);
            return authorizationService
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
