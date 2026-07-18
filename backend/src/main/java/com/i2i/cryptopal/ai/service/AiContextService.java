package com.i2i.cryptopal.ai.service;

import com.i2i.cryptopal.common.exception.ResourceNotFoundException;
import com.i2i.cryptopal.market.model.CurrentPriceQuote;
import com.i2i.cryptopal.market.service.CurrentPriceService;
import com.i2i.cryptopal.market.entity.PriceHistory;
import com.i2i.cryptopal.market.repository.PriceHistoryRepository;
import com.i2i.cryptopal.portfolio.entity.PortfolioAsset;
import com.i2i.cryptopal.portfolio.repository.PortfolioAssetRepository;
import com.i2i.cryptopal.trade.entity.TradeTransaction;
import com.i2i.cryptopal.trade.repository.TradeTransactionRepository;
import com.i2i.cryptopal.user.entity.AppUser;
import com.i2i.cryptopal.user.repository.AppUserRepository;
import com.i2i.cryptopal.wallet.entity.Wallet;
import com.i2i.cryptopal.wallet.repository.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AiContextService {

    private static final Set<String> SUPPORTED_SYMBOLS =
        Set.of("BTC", "ETH", "SOL");

    private final AppUserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PortfolioAssetRepository assetRepository;
    private final TradeTransactionRepository transactionRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CurrentPriceService currentPriceService;

    public AiContextService(
        AppUserRepository userRepository,
        WalletRepository walletRepository,
        PortfolioAssetRepository assetRepository,
        TradeTransactionRepository transactionRepository,
        PriceHistoryRepository priceHistoryRepository,
        CurrentPriceService currentPriceService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.assetRepository = assetRepository;
        this.transactionRepository = transactionRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.currentPriceService = currentPriceService;
    }

    @Transactional(readOnly = true)
    public String buildPrompt(
        Long userId,
        String question
    ) {
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User was not found"
            ));

        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet was not found"
            ));

        List<PortfolioAsset> assets =
            assetRepository.findByUserIdOrderBySymbolAsc(userId);

        PageRequest transactionPage = PageRequest.of(
            0,
            10,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        );

        List<TradeTransaction> transactions =
            transactionRepository.findByUserId(
                userId,
                transactionPage
            );

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are CryptoPal, an assistant embedded in a simulated
            cryptocurrency trading application.

            Rules:
            - Answer only questions about this user's account, portfolio,
              recent transactions, supplied market prices, and supplied
              price trends.
            - Use only the data supplied below. Do not invent balances,
              trades, prices, dates, or market events.
            - Clearly state when the supplied data is insufficient.
            - Do not promise future returns or guaranteed profits.
            - Do not present the response as professional financial advice.
            - Keep the answer concise, practical, and readable.
            - Respond in the same language as the user's question.
            - Markdown is allowed.

            USER ACCOUNT
            """);

        prompt.append("Username: ")
            .append(user.getUsername())
            .append('\n');

        prompt.append("Email: ")
            .append(user.getEmail())
            .append('\n');

        prompt.append("Cash balance: ")
            .append(wallet.getCashBalance())
            .append("\n\n");

        prompt.append("CURRENT MARKET PRICES\n");

        SUPPORTED_SYMBOLS.stream()
            .sorted()
            .forEach(symbol -> {
                CurrentPriceQuote quote =
                    currentPriceService.getCurrentPrice(symbol);

                prompt.append("- ")
                    .append(quote.symbol())
                    .append(": ")
                    .append(quote.price())
                    .append('\n');
            });

        prompt.append("\nPORTFOLIO ASSETS\n");

        if (assets.isEmpty()) {
            prompt.append("- No cryptocurrency assets\n");
        } else {
            for (PortfolioAsset asset : assets) {
                CurrentPriceQuote quote =
                    currentPriceService.getCurrentPrice(
                        asset.getSymbol()
                    );

                prompt.append("- ")
                    .append(asset.getSymbol())
                    .append(": quantity=")
                    .append(asset.getQuantity())
                    .append(", currentPrice=")
                    .append(quote.price())
                    .append('\n');
            }
        }

        prompt.append("\nRECENT TRANSACTIONS\n");

        if (transactions.isEmpty()) {
            prompt.append("- No completed transactions\n");
        } else {
            for (TradeTransaction transaction : transactions) {
                prompt.append("- ")
                    .append(transaction.getCreatedAt())
                    .append(" | ")
                    .append(transaction.getType())
                    .append(" | ")
                    .append(transaction.getSymbol())
                    .append(" | quantity=")
                    .append(transaction.getQuantity())
                    .append(" | executionPrice=")
                    .append(transaction.getExecutionPrice())
                    .append(" | total=")
                    .append(transaction.getTotalAmount())
                    .append('\n');
            }
        }

        prompt.append("\nRECENT PRICE HISTORY\n");

        PageRequest historyPage = PageRequest.of(
            0,
            10,
            Sort.by(
                Sort.Direction.DESC,
                "recordedAt"
            )
        );

        SUPPORTED_SYMBOLS.stream()
            .sorted()
            .forEach(symbol -> {
                List<PriceHistory> history =
                    priceHistoryRepository.findBySymbol(
                        symbol,
                        historyPage
                    );

                prompt.append(symbol).append(":\n");

                if (history.isEmpty()) {
                    prompt.append("- No historical records\n");
                } else {
                    history.forEach(item ->
                        prompt.append("- ")
                            .append(item.getRecordedAt())
                            .append(": ")
                            .append(item.getPrice())
                            .append('\n')
                    );
                }
            });

        prompt.append("\nUSER QUESTION\n")
            .append(question.trim())
            .append("\n\nANSWER\n");

        return prompt.toString();
    }
}