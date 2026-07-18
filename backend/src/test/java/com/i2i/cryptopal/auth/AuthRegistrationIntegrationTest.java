package com.i2i.cryptopal.auth;

import com.i2i.cryptopal.auth.dto.RegisterRequest;
import com.i2i.cryptopal.auth.dto.RegisterResponse;
import com.i2i.cryptopal.auth.service.AuthService;
import com.i2i.cryptopal.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void persistsANewUserAndCreatesAnInitialWallet() {
        String uniqueValue = String.valueOf(System.nanoTime());
        RegisterRequest request = new RegisterRequest(
            "user" + uniqueValue,
            "user" + uniqueValue + "@example.com",
            "secure-password-123"
        );

        RegisterResponse response = authService.register(request);

        assertThat(response.userId()).isNotNull();
        assertThat(response.initialBalance()).isPositive();
        assertThat(userRepository.findById(response.userId())).isPresent();
        assertThat(userRepository.findById(response.userId()).orElseThrow().getWallet())
            .isNotNull();
    }
}