package com.example.bankcards.mapper;

import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToDto")
    UserDto toDto(User user);

    @Named("mapRolesToDto")
    default List<String> mapRolesToDto(Set<Role> roles) {
        if (roles == null) {
            return new ArrayList<>();
        }
        return roles.stream()
                .map(x -> x.getName().name())
                .toList();
    }
}
