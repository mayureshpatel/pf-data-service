package com.mayureshpatel.pfdataservice.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class RegistrationRequest {
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be 50 characters or less")
    @Pattern(regexp = "^\\w{0,50}$", message = "Username can only contain letters, numbers, and underscores")
    private final String username;

    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must be 100 characters or less")
    @Email(message = "Email must be valid")
    private final String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,100}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @ToString.Exclude
    private final String password;
}
