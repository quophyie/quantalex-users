package com.quantal.exchange.users.convertors.orika;

import com.quantal.exchange.users.enums.GenderEnum;
import com.quantal.exchange.users.models.Gender;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

public class GenderToGenderEnumOrikaBiConvertor extends BidirectionalConverter<Gender,GenderEnum> {
    @Override
    public GenderEnum convertTo(Gender gender, Type<GenderEnum> type, MappingContext mappingContext) {
        return GenderEnum.valueOf(gender.getName());
    }

    @Override
    public Gender convertFrom(GenderEnum genderEnum, Type<Gender> type, MappingContext mappingContext) {
        Gender gender = new Gender();
        gender.setName(genderEnum.name());
        return gender;
    }
}
