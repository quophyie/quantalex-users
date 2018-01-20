package com.quantal.exchange.users.argumentmatchers;

import com.quantal.exchange.users.dto.AuthRequestDto;
import org.mockito.ArgumentMatcher;

public class AuthRequestDtoArgMatcher  extends ArgumentMatcher<AuthRequestDto> {
    private AuthRequestDto authRequestDto;
    public AuthRequestDtoArgMatcher(AuthRequestDto authRequestDto){
        this.authRequestDto = authRequestDto;
    }
    @Override
    public boolean matches(Object o) {
        return authRequestDto.getEmail().equals(((AuthRequestDto)o).getEmail())
                && authRequestDto.getTokenType().equals(((AuthRequestDto)o).getTokenType());
    }
}
