package com.innowise.userservice.service;

import com.innowise.userservice.dto.user.UserCreateRequest;
import com.innowise.userservice.dto.user.UserResponse;
import com.innowise.userservice.dto.user.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.specification.UserSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("User with email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("User created with id: {}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id,Long currentUserId, String role) {
        checkAccess(id, currentUserId, role);
        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    public Page<UserResponse> getAllUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(spec, pageable)
                .map(userMapper::toResponse);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request,Long currentUserId, String role) {
        checkAccess(id, currentUserId, role);
        log.info("Updating user with id: {}", id);

        User user = userRepository.findByIdWithCards(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userMapper.updateEntity(request, user);
        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);
        User user = findUserOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void activateUser(Long id) {
        log.info("Activating user with id: {}", id);
        User user = findUserOrThrow(id);
        user.setActive(true);
        userRepository.save(user);
    }
    private void checkAccess(Long userId, Long currentUserId, String role) {
        if (!"ADMIN".equals(role) && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
