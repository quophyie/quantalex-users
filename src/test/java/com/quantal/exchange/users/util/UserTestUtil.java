package com.quantal.exchange.users.util;

import com.quantal.exchange.users.dto.ApiGatewayUserRequestDto;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.models.User;

import java.time.LocalDate;

/**
 * Created by dman on 28/03/2017.
 */
public class UserTestUtil {
    public static User createUserModel(Long userId,
                                       String firstName,
                                       String lasstName,
                                       String email,
                                       String password,
                                       Gender gender,
                                       LocalDate dob) {

        User model = new User();
        model.setId(userId);
        model.setFirstName(firstName);
        model.setLastName(lasstName);
        model.setEmail(email);
        model.setGender(gender);
        model.setPassword(password);
        model.setDob(dob);
        return model;
    }

    public static UserDto createApiGatewayUserDto(Long userId,
                                                  String firstName,
                                                  String lasstName,
                                                  String email,
                                                  String password,
                                                  Gender gender,
                                                  LocalDate dob) {

        UserDto userDto = new UserDto();
        userDto.setId(userId);
        userDto.setFirstName(firstName);
        userDto.setLastName(lasstName);
        userDto.setEmail(email);
        userDto.setGender(gender);
        userDto.setPassword(password);
        userDto.setDob(dob);
        return userDto;
    }

    public static ApiGatewayUserRequestDto createApiGatewayUserDto(//Long customId,
                                                                   String username) {

        ApiGatewayUserRequestDto userDto = new ApiGatewayUserRequestDto();
      /* if(customId != null)
            userDto.setCustom_id(customId.toString());*/
        userDto.setUsername(username);
        return userDto;
    }

}
