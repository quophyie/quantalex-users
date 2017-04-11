package com.quantal.exchange.users.services.implementations;

import com.quantal.exchange.users.constants.MessageCodes;
import com.quantal.exchange.users.exceptions.AlreadyExistsException;
import com.quantal.exchange.users.exceptions.NotFoundException;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.objectmapper.OrikaBeanMapper;
import com.quantal.exchange.users.repositories.UserRepository;
import com.quantal.exchange.users.services.interfaces.MessageService;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Created by dman on 08/03/2017.
 */
@Service
public class UserServiceImpl implements UserService {

  private UserRepository userRepository;
  private MessageService messageService;
  protected OrikaBeanMapper nullSkippingMapper;

  @Autowired
  public UserServiceImpl(UserRepository userRepository,
                         MessageService messageService,
                         @Qualifier("nullSkippingOrikaBeanMapper")OrikaBeanMapper nullSkippingMapper){

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
  public User findOne(Long userid) {
    return userRepository.findOne(userid);
  }

  @Override
  public void delete(Long userId) {
    userRepository.delete(userId);
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

    if (userToUpdate == null){
      throw new NotFoundException(messageService.getMessage(MessageCodes.NOT_FOUND, new String[]{User.class.getSimpleName()}));
    }

    nullSkippingMapper.map(updateData, userToUpdate);
    return this.saveOrUpdate(userToUpdate);
  }

}
