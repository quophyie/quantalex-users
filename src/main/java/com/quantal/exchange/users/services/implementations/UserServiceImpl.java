package com.quantal.exchange.users.services.implementations;

import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.passay.RuleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserServiceImpl extends AbstractRepositoryServiceAsync<User, Long> implements UserService {

    private UserRepository userRepository;
    private MessageService messageService;
    private NullSkippingOrikaBeanMapper nullSkippingMapper;
    private ApiGatewayService apiGatewayService;
    //private OrikaBeanMapper orikaBeanMapper;
    private PasswordService passwordService;

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

        user.setEmail(user.getEmail().toLowerCase());
        return this.findOneByEmail(user.getEmail())
                .thenCompose(existingUser -> {


                    if (existingUser != null) {
                        String msg = String.format("user with email %s ", user.getEmail());
                        CompletableFuture completableFuture = new CompletableFuture();
                        AlreadyExistsException ex = new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                        completableFuture.completeExceptionally(ex);
                        return completableFuture;
                    }

                    RuleResult passwordValidationResult = passwordService.checkPasswordValidity(user.getPassword());

                    if (!passwordValidationResult.isValid()) {
                        String errMessages = passwordService
                                .getPasswordValidator()
                                .getMessages(passwordValidationResult)
                                .stream()
                                .reduce((s, s2) -> s + "\n" + s2).orElse("");

                        CompletableFuture completableFuture = new CompletableFuture();

                        completableFuture.completeExceptionally(new PasswordValidationException(errMessages));
                        return completableFuture;
                    } else {
                        String hashedPassword = passwordService.hashPassword(user.getPassword());
                        user.setPassword(hashedPassword);
                    }

                    ApiGatewayUserRequestDto gatewayUserDto = this.createApiGatewayUserDto(null, user.getEmail());
                    return apiGatewayService.addUer(gatewayUserDto)
                            .thenCompose(result -> this.saveOrUpdate(user));
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
    public CompletableFuture updateUser(User updateData) {

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

                    //If we are doing a password update
                    if (!StringUtils.isEmpty(updateData.getPassword())) {

                        RuleResult passwordValidationResult = passwordService.checkPasswordValidity(updateData.getPassword());

                        if (!passwordValidationResult.isValid()) {
                            String errMsg = passwordService
                                    .getPasswordValidator()
                                    .getMessages(passwordValidationResult)
                                    .stream()
                                    .reduce((s, s2) -> s + "\n" + s2).orElse("");


                            throw new PasswordValidationException(errMsg);
                        } else {
                            String hashedPassword = passwordService.hashPassword(updateData.getPassword());
                            userToUpdate.setPassword(hashedPassword);
                        }

                    }
                    return userToUpdate;
                })
                .thenApply(userToUpdate -> {

                    // Check and make sure that there isn't another user with the same email
                    // as the user we are about to update if the update data contains an email
                    if (StringUtils.isNotEmpty(updateData.getEmail())) {

                        return this
                                .findAllByEmailIgnoreCase(updateData.getEmail())
                                .thenApply(usersWithSameEmail -> {

                                    if (usersWithSameEmail.size() >= 1) {

                                        // Filter out users with the same email as the one we are about to update
                                        usersWithSameEmail.stream()
                                                .filter(user -> user.getId() != ((User) userToUpdate).getId())
                                                .forEach(user -> {
                                                    String msg = String.format("user with email %s ", updateData.getEmail());
                                                    throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                                                });
                                        //  String msg = String.format("user with email %s ", updateData.getEmail());
                                        //  throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                                    }
                                    return userToUpdate;
                                });
                    }
                    return userToUpdate;
                })
                .handle((result, ex) -> CommonUtils.processHandle(result, ex))
                .thenCompose(userToUpdate -> {
                    nullSkippingMapper.map(updateData, userToUpdate);
                    return this.saveOrUpdate((User) userToUpdate);
                });
    }

    private ApiGatewayUserRequestDto createApiGatewayUserDto(Long customId, String username) {
        ApiGatewayUserRequestDto userDto = new ApiGatewayUserRequestDto();
        userDto.setUsername(username.trim().toLowerCase());
        return userDto;
    }
}
