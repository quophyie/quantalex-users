package com.quantal.exchange.users.facades;

import com.quantal.exchange.users.dto.ResponseDTO;
import com.quantal.exchange.users.dto.UserDto;
import com.quantal.exchange.users.enums.Gender;
import com.quantal.exchange.users.models.User;
import com.quantal.exchange.users.services.api.GiphyApiService;
import com.quantal.exchange.users.services.interfaces.UserService;
import com.quantal.exchange.users.util.UserTestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    @InjectMocks
    private UserManagementFacade userManagementFacade;

    @Before
    public void setUp(){
     //userManagementFacade = new UserManagementFacade(userService, giphyApiService);
    }

    @Test
    public void shouldCreateNewUser() throws Exception {

        String persistedModelFirstName =  "createdUserFirstName";
        String persistedModelLastName = "createdUserLastName";
        String persistedModelEmail = "createdUser@quant.com";
        String persistedModelPassword = "createdUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);
        Long id = 1L;

        User createdModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        User userModelFromDto = UserTestUtil.createUserModel(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);


        UserDto createUserDto = UserTestUtil.createUserDto(null,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, dob);

        given(this.userService
                .saveOrUpdate(eq(userModelFromDto)))
                .willReturn(createdModel);

        ResponseEntity<?> responseEntity = userManagementFacade.save(createUserDto);
        UserDto result = ((ResponseDTO<UserDto>)responseEntity.getBody()).getData();
        String message = ((ResponseDTO<UserDto>)responseEntity.getBody()).getMessage();

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.CREATED);
        assertThat("User created successfully").isEqualToIgnoringCase(message);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(persistedModelFirstName);
        assertThat(result.getLastName()).isEqualTo(persistedModelLastName);
        assertThat(result.getEmail()).isEqualTo(persistedModelEmail);
        assertThat(result.getPassword()).isEqualTo(persistedModelPassword);
        assertThat(result.getDob()).isEqualTo(dob);
        assertThat(result.getGender()).isEqualTo(Gender.M);

        verify(userService, times(1)).saveOrUpdate(userModelFromDto);
    }


    @Test
    public void shouldUpdateUserWithPartialData() throws Exception {

        String persistedModelFirstName =  "persistedDtoFirstName";
        String persistedModelLastName = "persistedDtoLastName";
        String persistedModelEmail = "persistedModel@quant.com";
        String persistedModelPassword = "persistedDtoPassword";
        String updateDtoFirstName = "updatedFirstName";
        String updateDtoLastName = "updatedLastName";
        String updateDtoEmail = "updateModel@quant.com";
        Long id = 1L;

        User persistedModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);

        User updateModel = UserTestUtil.createUserModel(id,
                updateDtoFirstName,
                updateDtoLastName,
                updateDtoEmail,
                null,
                null,
                null);

        UserDto updateDto = UserTestUtil.createUserDto(id,
                updateDtoFirstName,
                updateDtoLastName,
                updateDtoEmail,
                null,
                null,
                null);


        given(this.userService
                .findOne(id))
                .willReturn(persistedModel);

        given(this.userService
                .saveOrUpdate(eq(updateModel)))
                .willReturn(persistedModel);

        ResponseEntity<?> responseEntity = userManagementFacade.update(id, updateDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);

        UserDto result = ((ResponseDTO<UserDto>)responseEntity.getBody()).getData();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(updateDtoFirstName);
        assertThat(result.getLastName()).isEqualTo(updateDtoLastName);
        assertThat(result.getEmail()).isEqualTo(updateDtoEmail);
        assertThat(result.getPassword()).isEqualTo(persistedModelPassword);
        assertThat(result.getGender()).isEqualTo(Gender.M);
        verify(userService, times(1)).saveOrUpdate(updateModel);
        verify(userService, times(1)).findOne(id);
    }

    @Test
    public void shouldUpdateUserWithFullData() throws Exception {

        Long id = 1L;
        String persistedModelFirstName =  "persistedUserFirstName";
        String persistedModelLastName = "persistedUserLastName";
        String persistedModelEmail = "persistedUser@quant.com";
        String persistedModelPassword = "persistedUserPassword";
        LocalDate dob = LocalDate.of(1990, 01, 01);

        String updatedUserFirstName =  "updatedUserFirstName";
        String updatedUserLastName = "updatedUserLastName";
        String updatedEmail = "updatedUser@quant.com";
        String updatedUserPassword = "updatedUserPassword";
        LocalDate updatedDob = LocalDate.of(1980, 11, 01);



        User persistedModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M,
                dob);

        User updateModel = UserTestUtil.createUserModel(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                Gender.F,
                updatedDob);

        UserDto updateDto = UserTestUtil.createUserDto(id,
                updatedUserFirstName,
                updatedUserLastName,
                updatedEmail,
                updatedUserPassword,
                Gender.F,
                updatedDob);


        given(this.userService
                .findOne(id))
                .willReturn(persistedModel);

        given(this.userService
                .saveOrUpdate(eq(updateModel)))
                .willReturn(persistedModel);

        ResponseEntity<?> responseEntity = userManagementFacade.update(id, updateDto);

        HttpStatus httpStatusCode  = responseEntity.getStatusCode();
        assertThat(httpStatusCode).isEqualTo(HttpStatus.OK);

        UserDto result = ((ResponseDTO<UserDto>)responseEntity.getBody()).getData();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getFirstName()).isEqualTo(updatedUserFirstName);
        assertThat(result.getLastName()).isEqualTo(updatedUserLastName);
        assertThat(result.getEmail()).isEqualTo(updatedEmail);
        assertThat(result.getPassword()).isEqualTo(updatedUserPassword);
        assertThat(result.getGender()).isEqualTo(Gender.F);
        verify(userService, times(1)).saveOrUpdate(updateModel);
        verify(userService, times(1)).findOne(id);
    }

    @Test
    public void should404NotFoundGivenNullUserDtoOnUserUpdate() throws Exception {

        String persistedModelFirstName = "persistedDtoFirstName";
        String persistedModelLastName = "persistedDtoLastName";
        String persistedModelEmail = "persistedModel@quant.com";
        String persistedModelPassword = "persistedDtoPassword";
        String updateDtoFirstName = "updatedFirstName";
        String updateDtoLastName = "updatedLastName";
        String updateDtoEmail = "updateModel@quant.com";
        Long id = 1L;

        User persistedModel = UserTestUtil.createUserModel(id,
                persistedModelFirstName,
                persistedModelLastName,
                persistedModelEmail,
                persistedModelPassword,
                Gender.M, null);
    }



}
