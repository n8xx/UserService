package com.example.userservice.mapper;

import com.example.userservice.dto.user.UserCreateRequest;
import com.example.userservice.dto.user.UserResponse;
import com.example.userservice.dto.user.UserUpdateRequest;
import com.example.userservice.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "cards", source = "paymentCards")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}