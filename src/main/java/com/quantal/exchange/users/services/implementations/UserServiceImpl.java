package com.quantal.exchange.users.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
import com.quantal.exchange.users.dto.ApiGatewayUserResponseDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialRequestDto;
import com.quantal.exchange.users.dto.ApiJwtUserCredentialResponseDto;
import com.quantal.exchange.users.exceptions.InvalidDataException;
import com.quantal.exchange.users.exceptions.PasswordValidationException;
import com.quantal.exchange.users.services.api.ApiGatewayService;
import com.quantal.exchange.users.services.interfaces.PasswordService;
import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.shared.objectmapper.OrikaBeanMapper;
import com.quantal.shared.services.implementations.AbstractRepositoryServiceAsync;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.shared.util.CommonUtils;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.passay.RuleResult;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserServiceImpl extends AbstractRepositoryServiceAsync<User, Long> implements UserService {

    private final XLogger logger = XLoggerFactory.getXLogger(this.getClass().getName());

    private UserRepository userRepository;
    private MessageService messageService;
    private NullSkippingOrikaBeanMapper nullSkippingMapper;
    private ApiGatewayService apiGatewayService;
    //private OrikaBeanMapper orikaBeanMapper;
    private PasswordService passwordService;

    @Value("#{environment.JWT_SECRET}")
    private String JWT_SECRET;

    @Autowired
    private ExecutorService taskExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("#{environment.JWT_ALGORITHM}") String JWT_ALGORITHM;

    @Value("#{environment.JWT_TYPE}") String JWT_TYPE;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           MessageService messageService,
                           OrikaBeanMapper orikaBeanMapper,
                           NullSkippingOrikaBeanMapper nullSkippingMapper,
                           ApiGatewayService apiGatewayService,
                           PasswordService passwordService) {
        super(userRepository, orikaBeanMapper, nullSkippingMapper);

        this.userRepository = userRepository;
        this.messageService = messageService;
        this.nullSkippingMapper = nullSkippingMapper;
        //this.orikaBeanMapper = orikaBeanMapper;
        this.apiGatewayService = apiGatewayService;
        this.passwordService = passwordService;
    }

    @Override
    public CompletableFuture<User> createUser(User user) {
        if (!ObjectUtils.allNotNull(user)) {
            throw new NullPointerException(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
        }

        ApiGatewayUserResponseDto gatewayUserResponse = new ApiGatewayUserResponseDto();
        user.setEmail(user.getEmail().toLowerCase());
         return this.findOneByEmail(user.getEmail())
                .thenCompose(existingUser -> {

                    if (existingUser != null) {
                        String msg = String.format("user with email %s ", user.getEmail());
                        throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                    }

                    checkAndSetPassword(user.getPassword(),user);
                    ApiGatewayUserRequestDto gatewayUserDto = this.createApiGatewayUserDto(null, user.getEmail());

                    return apiGatewayService.addUer(gatewayUserDto)
                            .thenCompose(gwayUserResponse -> {
                                nullSkippingMapper.map(gwayUserResponse, gatewayUserResponse);
                                user.setApiUserId(gwayUserResponse.getId());
                               return this.saveOrUpdate(user);
                            });
                });

    }

    @Override
    public CompletableFuture<User> saveOrUpdate(User user) {
        user.setJoinDate(LocalDate.now());
        return CompletableFuture.completedFuture(userRepository.save(user));
    }

    @Override
    public CompletableFuture<User> findOneByEmail(String email) {
        return CompletableFuture.completedFuture(userRepository.findOneByEmail(email));
    }

    @Override
    public CompletableFuture<Long> countByEmailIgnoreCase(String email) {
        return CompletableFuture.completedFuture(userRepository.countByEmailIgnoreCase(email));
    }

    @Override
    public CompletableFuture<List<User>> findAllByEmailIgnoreCase(String email) {
        return CompletableFuture.completedFuture(userRepository.findAllByEmailIgnoreCase(email));
    }

    @Override
    public CompletableFuture<User> findOne(Long userid) {
        return CompletableFuture.completedFuture(userRepository.findOne(userid));
    }

    @Override
    public CompletableFuture<Void> deleteById(Long userId) {

        return CompletableFuture.runAsync(() -> userRepository.delete(userId))
                .exceptionally((exception) -> {
                    if (exception.getCause() instanceof EmptyResultDataAccessException) {
                        throw new NotFoundException("");
                    }
                    throw new RuntimeException(exception.getCause());
                });
    }

    @Override
    public CompletableFuture<User> updateUser(User updateData) {

        if (!ObjectUtils.allNotNull(updateData)) {
            String errMsg = String.format(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
            throw new NullPointerException(errMsg);
        }

        Long userId = updateData.getId();
        //User userToUpdate = null;
        if (userId == null) {
            String errMsg = String.format("UserId of update object %s", messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
            throw new NullPointerException(errMsg);
        }

        return this.findOne(userId)
                .thenApply(userToUpdate -> {

                    if (userToUpdate == null) {
                        throw new NotFoundException(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
                    }
                    return checkAndSetPassword(updateData.getPassword(), updateData);
                })
                .handle((user, exception) -> CommonUtils.processHandle(user, exception))
                .thenCompose(userToUpdate -> checkEmailAvailability(updateData.getEmail(), userToUpdate))
                .handle((result, ex) -> CommonUtils.processHandle(result, ex))
                .thenCompose(userToUpdate -> {
                     nullSkippingMapper.map(updateData, userToUpdate);
                    return this.saveOrUpdate(userToUpdate);
                });
    }

    private ApiGatewayUserRequestDto createApiGatewayUserDto(Long customId, String username) {
        ApiGatewayUserRequestDto userDto = new ApiGatewayUserRequestDto();
        userDto.setUsername(username.trim().toLowerCase());
        return userDto;
    }

    private ApiJwtUserCredentialRequestDto createApiJwtUserCredentialRequestDto() {
        ApiJwtUserCredentialRequestDto userDto = new ApiJwtUserCredentialRequestDto();
        userDto.setAlgorithm(JWT_ALGORITHM);
        userDto.setSecret(JWT_SECRET);
        return userDto;
    }

    private CompletableFuture<User> checkEmailAvailability (String email, User userToUpdate) {

            // Check and make sure that there isn't another user with the same email
            // as the user we are about to update if the update data contains an email
            if (StringUtils.isNotEmpty(email)) {

                return this
                        .findAllByEmailIgnoreCase(email)
                        .thenApply(usersWithSameEmail -> {

                            if (usersWithSameEmail.size() >= 1) {

                                // Filter out users with the same email as the one we are about to update
                                usersWithSameEmail.stream()
                                        .filter(user -> user.getId() != ((User) userToUpdate).getId())
                                        .forEach(user -> {
                                            String msg = String.format("user with email %s ", email);
                                            throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                                        });
                                //  String msg = String.format("user with email %s ", updateData.getEmail());
                                //  throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                            }
                            return userToUpdate;
                        });
            }
            return CompletableFuture.completedFuture(userToUpdate);
        }



        private User checkAndSetPassword(String password, User user) {

            if (user == null) {
                throw new NotFoundException(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
            }

            //If we are doing a password update
            if (!StringUtils.isEmpty(password)) {

                RuleResult passwordValidationResult = passwordService.checkPasswordValidity(password);

                if (passwordValidationResult != null && !passwordValidationResult.isValid()) {
                    String errMsg = passwordService
                            .getPasswordValidationCheckErrorMessages(passwordValidationResult,"\n");
                    throw new PasswordValidationException(errMsg);
                } else {
                    String hashedPassword = passwordService.hashPassword(password);
                    user.setPassword(hashedPassword);
                }
            }
            return user;
        }

        public CompletableFuture<ApiJwtUserCredentialResponseDto> requestApiGatewayUserCredentials(String username){
             ApiJwtUserCredentialRequestDto requestDto = createApiJwtUserCredentialRequestDto();
            return apiGatewayService.requestConsumerJwtCredentials(username, requestDto);
        }


    /**
     * Creates a JWT
     * @param issuer - The issuer. If you are using kong, then this will be the JWT Credential's Key
     * @return
     */
    public String createJwt(String issuer) {

        if (StringUtils.isEmpty(issuer)){
            String message = messageService.getMessage(MessageCodes.NULL_OR_EMPTY_DATA, new String[] {"issuer"});
            logger.throwing(new InvalidDataException(message));
        }
            try {

                logger.debug("creating JWT with issuer: {}", issuer);

                JwsHeader header = Jwts.jwsHeader();
                header.setAlgorithm(JWT_ALGORITHM);
                header.setType(JWT_TYPE);

                String keyAsBase64 = Base64.getEncoder().encodeToString(JWT_SECRET.getBytes("utf-8"));

                String compactJws = Jwts.builder()
                        .setHeader((Map<String, Object>) header)
                        .setIssuer(issuer)
                        .claim("type", "user")
                        .signWith(SignatureAlgorithm.HS256, keyAsBase64)
                        .compact();
                logger.debug("created JWT: {}", compactJws);
                return compactJws;
            } catch (UnsupportedEncodingException uee){
              logger.debug("Error encoding JWT secret:", uee);
              throw new RuntimeException(uee.getMessage(), uee.getCause());
            }
        }
}
