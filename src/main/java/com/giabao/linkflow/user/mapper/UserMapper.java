package com.giabao.linkflow.user.mapper;

import com.giabao.linkflow.user.dto.req.CreateUserRequest;
import com.giabao.linkflow.user.dto.req.UpdateUserRequest;
import com.giabao.linkflow.user.dto.res.UserResponse;
import com.giabao.linkflow.user.enity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    @Mapping(target = "email", ignore = true)
    @Mapping(target = "links", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "email", ignore = true)
    @Mapping(target = "links", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
