package com.i2i.cryptopal.demo;

import com.i2i.cryptopal.auth.dto.LoginRequest;
import com.i2i.cryptopal.auth.dto.LoginResponse;
import com.i2i.cryptopal.auth.dto.RegisterRequest;
import com.i2i.cryptopal.auth.service.AuthService;
import com.i2i.cryptopal.market.service.MarketService;
import com.i2i.cryptopal.trade.dto.TradeRequest;
import com.i2i.cryptopal.trade.service.TradeService;
import com.i2i.cryptopal.user.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(1)
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final AuthService authService;
    private final TradeService tradeService;
    private final MarketService marketService;
    private final AppUserRepository userRepository;
    private final String password;

    public DemoDataSeeder(
        AuthService authService,
        TradeService tradeService,
        MarketService marketService,
        AppUserRepository userRepository,
        @Value("${app.demo-data.password}") String password
    ) {
        this.authService = authService;
        this.tradeService = tradeService;
        this.marketService = marketService;
        this.userRepository = userRepository;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        marketService.getLatestPrices();
        seedUser("demo.btc", "demo.btc@cryptopal.local", "BTC", "0.05000000");
        seedUser("demo.eth", "demo.eth@cryptopal.local", "ETH", "1.25000000");
        seedUser("demo.sol", "demo.sol@cryptopal.local", "SOL", "10.00000000");
    }

    private void seedUser(String username, String email, String symbol, String quantity) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        authService.register(new RegisterRequest(username, email, password));
        LoginResponse login = authService.login(new LoginRequest(email, password));
        tradeService.buy(
            "Bearer " + login.token(),
            new TradeRequest(symbol, new BigDecimal(quantity))
        );
    }
}