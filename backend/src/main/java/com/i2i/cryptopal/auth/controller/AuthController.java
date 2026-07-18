package com.i2i.cryptopal.auth.controller;

import com.i2i.cryptopal.auth.service.AuthService;

import com.i2i.cryptopal.auth.dto.LoginRequest;
import com.i2i.cryptopal.auth.dto.LoginResponse;
import com.i2i.cryptopal.auth.dto.RegisterRequest;
import com.i2i.cryptopal.auth.dto.RegisterResponse;
import com.i2i.cryptopal.auth.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
        @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @GetMapping("/session")
    public SessionResponse getSession(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader
    ) {
        return authService.getCurrentSession(authorizationHeader);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader
    ) {
        authService.logout(authorizationHeader);
    }
}