package com.innowise.userservice.service;

import com.innowise.userservice.dto.user.UserCreateRequest;
import com.innowise.userservice.dto.user.UserUpdateRequest;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.dto.user.UserResponse;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final String ADMIN = "ADMIN";
    private static final String USER = "USER";

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
                .id(USER_ID)
                .name("Anna")
                .surname("Ivanova")
                .email("anna@innowise.com")
                .birthDate(LocalDate.of(1995, 5, 15))
                .active(true)
                .build();

        userResponse = UserResponse.builder()
                .id(USER_ID)
                .name("Anna")
                .surname("Ivanova")
                .email("anna@innowise.com")
                .active(true)
                .build();

        createRequest = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "anna@innowise.com"
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
        assertThat(result.getEmail()).isEqualTo("anna@innowise.com");
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
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(USER_ID, USER_ID, USER);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void getUserById_successForAdmin() {
        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(USER_ID, OTHER_USER_ID, ADMIN);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
    }

    @Test
    void getUserById_throwsAccessDenied_forNonOwner() {
        assertThatThrownBy(() -> userService.getUserById(USER_ID, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getUserById_notFound_throwsException() {
        when(userRepository.findByIdWithCards(OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(OTHER_USER_ID, OTHER_USER_ID, USER))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_success() {
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(USER_ID, updateRequest, USER_ID, USER);

        assertThat(result).isNotNull();
        verify(userMapper).updateEntity(updateRequest, user);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_successForAdmin() {
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.updateUser(USER_ID, updateRequest, OTHER_USER_ID, ADMIN);

        assertThat(result).isNotNull();
    }

    @Test
    void updateUser_throwsAccessDenied_forNonOwner() {
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        assertThatThrownBy(() -> userService.updateUser(USER_ID, updateRequest, OTHER_USER_ID, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deactivateUser_success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deactivateUser(USER_ID);

        assertThat(user.getActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_success() {
        user.setActive(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.activateUser(USER_ID);

        assertThat(user.getActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_notFound_throwsException() {
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(OTHER_USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getAllUsers_noFilters_returnsAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Anna");
    }

    @Test
    void getAllUsers_withNameFilter_returnsMatchingUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers("Anna", null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllUsers_withSurnameFilter_returnsMatchingUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(null, "Ivanova", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAllUsers_noMatch_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        Page<UserResponse> result = userService.getAllUsers("Unknown", null, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}