package com.example.userservice.service;

import com.example.userservice.dto.user.UserCreateRequest;
import com.example.userservice.dto.user.UserResponse;
import com.example.userservice.dto.user.UserUpdateRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.BusinessException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;
    private UserCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Anna")
                .surname("Ivanova")
                .email("anna@example.com")
                .birthDate(LocalDate.of(1995, 5, 15))
                .active(true)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .name("Anna")
                .surname("Ivanova")
                .email("anna@example.com")
                .active(true)
                .build();

        createRequest = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "anna@example.com"
        );
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userMapper.toEntity(createRequest)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.createUser(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("anna@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_emailAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_success() {
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(userMapper).updateEntity(updateRequest, user);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertThat(user.getActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_success() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.activateUser(1L);

        assertThat(user.getActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }
}