package com.quantal.exchange.users.convertors.orika;

import com.quantal.exchange.users.enums.UserStatusEnum;
import com.quantal.exchange.users.models.UserStatus;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

public class UserStatusToUserStatusEnumOrikaBiConvertor extends BidirectionalConverter<UserStatus,UserStatusEnum> {
    @Override
    public UserStatusEnum convertTo(UserStatus userStatus, Type<UserStatusEnum> type, MappingContext mappingContext) {


        return UserStatusEnum.valueOf(userStatus.getStatus());
    }

    @Override
    public UserStatus convertFrom(UserStatusEnum userStatusEnum, Type<UserStatus> type, MappingContext mappingContext) {
        UserStatus userStatus = new UserStatus();
        userStatus.setStatus(userStatusEnum.name());
        return userStatus;
    }
}
