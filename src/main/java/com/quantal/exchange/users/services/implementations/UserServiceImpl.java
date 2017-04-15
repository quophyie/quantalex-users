package com.quantal.exchange.users.services.implementations;

import com.quantal.shared.objectmapper.NullSkippingOrikaBeanMapper;
import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.shared.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserServiceImpl implements UserService {

  private UserRepository userRepository;
  private MessageService messageService;
  private NullSkippingOrikaBeanMapper nullSkippingMapper;

  @Autowired
  public UserServiceImpl(UserRepository userRepository,
                         MessageService messageService,
                         NullSkippingOrikaBeanMapper nullSkippingMapper){

   this.userRepository = userRepository;
   this.messageService = messageService;
   this.nullSkippingMapper = nullSkippingMapper;
  }

  @Override
  public User createUser(User user) {
    if (!ObjectUtils.allNotNull(user)){
      throw new NullPointerException(messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
    }

    User existingUser = this.findOneByEmail(user.getEmail());

    if(existingUser != null ){
      String msg = String.format("user with email %s ", user.getEmail());
      throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
    }

    return this.saveOrUpdate(user);

  }

  @Override
  public User saveOrUpdate(User user) {
    user.setJoinDate(LocalDate.now());
    return userRepository.save(user);
  }

  @Override
  public User findOneByEmail(String email) {
    return userRepository.findOneByEmail(email);
  }

  @Override
  public Long countByEmailIgnoreCase(String email) {
    return userRepository.countByEmailIgnoreCase(email);
  }

  @Override
  public List<User> findAllByEmailIgnoreCase(String email){
    return userRepository.findAllByEmailIgnoreCase(email);
  }
  @Override
  public User findOne(Long userid) {
    return userRepository.findOne(userid);
  }

  @Override
  public void deleteById(Long userId) {
    try {
      userRepository.delete(userId);
    } catch (EmptyResultDataAccessException erdae) {
      throw new NotFoundException("");
    }
  }

  @Override
  public User updateUser(User updateData) {
    if (!ObjectUtils.allNotNull(updateData)){
      String errMsg = String.format("Update object %s", messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
      throw new NullPointerException(errMsg);
    }

    Long userId = updateData.getId();
    if (userId == null){
      String errMsg = String.format("UserId of update object %s", messageService.getMessage(MessageCodes.NULL_DATA_PROVIDED));
      throw new NullPointerException(errMsg);
    }

    User userToUpdate = this.findOne(userId);

    // Check and make sure that there isn't another user with the same email
    // as the user we are about to update if the update data contains an email
    if (StringUtils.isNotEmpty(updateData.getEmail())) {
      List<User> usersWithSameEmail = this.findAllByEmailIgnoreCase(updateData.getEmail());
      if (usersWithSameEmail.size() >= 1){

        // Filter out users with the same email as the one we are about to update
        usersWithSameEmail.stream()
                .filter(user -> user.getId() != userToUpdate.getId())
                .forEach( user -> {
                  String msg = String.format("user with email %s ", updateData.getEmail());
                  throw new AlreadyExistsException(messageService.getMessage(MessageCodes.ENTITY_ALREADY_EXISTS, new String[]{msg}));
                });
      }
    }

    if (userToUpdate == null){
      throw new NotFoundException(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
    }

    nullSkippingMapper.map(updateData, userToUpdate);
    return this.saveOrUpdate(userToUpdate);
  }

}
