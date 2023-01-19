package org.komponente.userservice.mapper;

import org.komponente.dto.user.UserDto;
import org.komponente.userservice.domain.User;

public class UserMapper {
    public static UserDto userToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        userDto.setSurname(user.getSurname());
        userDto.setHasaccess(user.getHasaccess());
        return userDto;
    }
}
