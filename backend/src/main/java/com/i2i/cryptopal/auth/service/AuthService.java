package com.i2i.cryptopal.auth.service;

import com.i2i.cryptopal.auth.dto.LoginRequest;
import com.i2i.cryptopal.auth.dto.LoginResponse;
import com.i2i.cryptopal.auth.dto.RegisterRequest;
import com.i2i.cryptopal.auth.dto.RegisterResponse;
import com.i2i.cryptopal.auth.dto.SessionResponse;
import com.i2i.cryptopal.common.exception.DuplicateResourceException;
import com.i2i.cryptopal.common.exception.InvalidCredentialsException;
import com.i2i.cryptopal.common.exception.UnauthorizedException;
import com.i2i.cryptopal.user.entity.AppUser;
import com.i2i.cryptopal.user.repository.AppUserRepository;
import com.i2i.cryptopal.wallet.entity.Wallet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final long MINIMUM_INITIAL_BALANCE = 5_000L;
    private static final int INITIAL_BALANCE_RANGE = 15_001;

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        SessionService sessionService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException(
                "This username is already registered"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                "This email address is already registered"
            );
        }

        BigDecimal initialBalance = generateInitialBalance();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCashBalance(initialBalance);
        user.setWallet(wallet);

        AppUser savedUser = userRepository.save(user);

        return new RegisterResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            initialBalance
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();

        AppUser user = findByIdentifier(identifier)
            .orElseThrow(() -> new InvalidCredentialsException(
                "Username, email or password is incorrect"
            ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(
                "Username, email or password is incorrect"
            );
        }

        String token = sessionService.createSession(user);

        return new LoginResponse(
            token,
            "Bearer",
            sessionService.getExpirationSeconds(),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getWallet().getCashBalance()
        );
    }

    @Transactional(readOnly = true)
    public SessionResponse getCurrentSession(String authorizationHeader) {
        Long userId = sessionService.requireUserId(authorizationHeader);

        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException(
                "The user belonging to this session no longer exists"
            ));

        return new SessionResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getWallet().getCashBalance()
        );
    }

    public void logout(String authorizationHeader) {
        sessionService.deleteSession(authorizationHeader);
    }

    private Optional<AppUser> findByIdentifier(String identifier) {
        Optional<AppUser> userByEmail =
            userRepository.findByEmailIgnoreCase(identifier);

        if (userByEmail.isPresent()) {
            return userByEmail;
        }

        return userRepository.findByUsernameIgnoreCase(identifier);
    }

    private BigDecimal generateInitialBalance() {
        long amount = MINIMUM_INITIAL_BALANCE
            + secureRandom.nextInt(INITIAL_BALANCE_RANGE);

        return BigDecimal.valueOf(amount).setScale(2);
    }
}