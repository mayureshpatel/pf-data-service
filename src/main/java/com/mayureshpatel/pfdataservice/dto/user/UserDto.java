package com.mayureshpatel.pfdataservice.dto.user;

import com.mayureshpatel.pfdataservice.domain.user.User;

public record UserDto(
        Long id,
        String username,
        String email
) {

    /**
     * Maps a {@link User} domain object to its corresponding DTO representation.
     *
     * @param user The User domain object to be mapped.
     * @return The {@link UserDto} representation of the provided User.
     */
    public static UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}
