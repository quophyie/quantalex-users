package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.ResponseDTO;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;

/**
 * Created by dman on 25/03/2017.
 */
@RunWith(SpringRunner.class)
//@WebMvcTest(UserManagementFacade.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserManagementFacadesTest {

    @MockBean
    private GiphyApiService giphyApiService;
    @MockBean
    private UserService userService;


    @Autowired
   // @InjectMocks
    private UserManagementFacade userManagementFacade;

    @Before
    public void setUp(){
     //userManagementFacade = new UserManagementFacade(userService, giphyApiService);
    }

    @Test
    public void shouldUpdateUserWithPartialData() throws Exception {

        User persistedModel = new User();
        User updateModel = new User();
        UserDto updateDto = new UserDto();

        String persistedModelFirstName =  "persistedDtoFirstName";
        String persistedModelLastName = "persistedDtoLastName";
        String persistedModelEmail = "persistedModel@quant.com";
        String persistedModelPassword = "persistedDtoPassword";
        String updateDtoFirstName = "updatedFirstName";
        String updateDtoLastName = "updatedLastName";
        String updateDtoEmail = "updateModel@quant.com";
        Long id = 1L;

        persistedModel.setId(id);
        persistedModel.setFirstName(persistedModelFirstName);
        persistedModel.setLastName(persistedModelLastName);
        persistedModel.setEmail(persistedModelEmail);
        persistedModel.setGender(Gender.M);
        persistedModel.setPassword(persistedModelPassword);

        updateModel.setId(id);
        updateModel.setFirstName(updateDtoFirstName);
        updateModel.setLastName(updateDtoLastName);
        updateModel.setEmail(updateDtoEmail);

        updateDto.setId(id);
        updateDto.setFirstName(updateDtoFirstName);
        updateDto.setLastName(updateDtoLastName);
        updateDto.setEmail(updateDtoEmail);

        given(this.userService
                .findOneByEmail(updateModel.getEmail()))
                .willReturn(persistedModel);

        given(this.userService
                .saveOrUpdate(eq(updateModel)))
                .willReturn(persistedModel);

        UserDto result = (UserDto)((ResponseDTO)userManagementFacade.update(updateDto).getBody()).getData();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(updateDtoFirstName);
        assertThat(result.getLastName()).isEqualTo(updateDtoLastName);
        assertThat(result.getEmail()).isEqualTo(updateDtoEmail);
        assertThat(result.getPassword()).isEqualTo(persistedModelPassword);
        assertThat(result.getGender()).isEqualTo(Gender.M);
    }

}
