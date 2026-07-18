package com.i2i.cryptopal.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must contain 3-50 characters")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must contain 8-72 characters")
    String password
) {
}
