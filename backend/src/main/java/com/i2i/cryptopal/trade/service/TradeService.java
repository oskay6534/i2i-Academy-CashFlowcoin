package com.i2i.cryptopal.trade.service;

import com.i2i.cryptopal.trade.entity.TradeTransaction;
import com.i2i.cryptopal.trade.model.TradeType;
import com.i2i.cryptopal.trade.repository.TradeTransactionRepository;

import com.i2i.cryptopal.auth.service.SessionService;
import com.i2i.cryptopal.analytics.service.TradeSearchIndexer;
import com.i2i.cryptopal.common.exception.InsufficientAssetException;
import com.i2i.cryptopal.common.exception.InsufficientFundsException;
import com.i2i.cryptopal.common.exception.InvalidTradeException;
import com.i2i.cryptopal.common.exception.ResourceNotFoundException;
import com.i2i.cryptopal.market.model.CurrentPriceQuote;
import com.i2i.cryptopal.market.service.CurrentPriceService;
import com.i2i.cryptopal.portfolio.entity.PortfolioAsset;
import com.i2i.cryptopal.portfolio.repository.PortfolioAssetRepository;
import com.i2i.cryptopal.portfolio.dto.PortfolioAssetResponse;
import com.i2i.cryptopal.portfolio.dto.PortfolioResponse;
import com.i2i.cryptopal.trade.dto.TradeRequest;
import com.i2i.cryptopal.trade.dto.TradeResponse;
import com.i2i.cryptopal.trade.dto.TransactionResponse;
import com.i2i.cryptopal.wallet.entity.Wallet;
import com.i2i.cryptopal.wallet.repository.WalletRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradeService {

    private static final int QUANTITY_SCALE = 8;
    private static final int MONEY_SCALE = 2;

    private final SessionService sessionService;
    private final CurrentPriceService currentPriceService;
    private final WalletRepository walletRepository;
    private final PortfolioAssetRepository assetRepository;
    private final TradeTransactionRepository transactionRepository;
    private final TradeSearchIndexer tradeSearchIndexer;

    public TradeService(
        SessionService sessionService,
        CurrentPriceService currentPriceService,
        WalletRepository walletRepository,
        PortfolioAssetRepository assetRepository,
        TradeTransactionRepository transactionRepository,
        TradeSearchIndexer tradeSearchIndexer
    ) {
        this.sessionService = sessionService;
        this.currentPriceService = currentPriceService;
        this.walletRepository = walletRepository;
        this.assetRepository = assetRepository;
        this.transactionRepository = transactionRepository;
        this.tradeSearchIndexer = tradeSearchIndexer;
    }

    @Transactional
    public TradeResponse buy(
        String authorizationHeader,
        TradeRequest request
    ) {
        Long userId = sessionService.requireUserId(
            authorizationHeader
        );

        Wallet wallet = getWalletForUpdate(userId);
        CurrentPriceQuote quote = currentPriceService
            .getCurrentPrice(request.symbol());

        BigDecimal quantity = normalizeQuantity(
            request.quantity()
        );

        BigDecimal totalAmount = calculateTotal(
            quote.price(),
            quantity
        );

        if (wallet.getCashBalance().compareTo(totalAmount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds to complete this trade"
            );
        }

        wallet.setCashBalance(
            wallet.getCashBalance()
                .subtract(totalAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        );

        PortfolioAsset asset = assetRepository
            .findForUpdate(userId, quote.symbol())
            .orElseGet(() -> {
                PortfolioAsset created = new PortfolioAsset();
                created.setUser(wallet.getUser());
                created.setSymbol(quote.symbol());
                created.setQuantity(
                    BigDecimal.ZERO.setScale(QUANTITY_SCALE)
                );
                return created;
            });

        asset.setQuantity(
            asset.getQuantity()
                .add(quantity)
                .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)
        );

        assetRepository.save(asset);

        TradeTransaction transaction = createTransaction(
            wallet,
            TradeType.BUY,
            quote,
            quantity,
            totalAmount
        );

        transactionRepository.save(transaction);
        tradeSearchIndexer.index(transaction);

        return toTradeResponse(
            transaction,
            wallet,
            asset.getQuantity()
        );
    }

    @Transactional
    public TradeResponse sell(
        String authorizationHeader,
        TradeRequest request
    ) {
        Long userId = sessionService.requireUserId(
            authorizationHeader
        );

        Wallet wallet = getWalletForUpdate(userId);
        CurrentPriceQuote quote = currentPriceService
            .getCurrentPrice(request.symbol());

        BigDecimal quantity = normalizeQuantity(
            request.quantity()
        );

        PortfolioAsset asset = assetRepository
            .findForUpdate(userId, quote.symbol())
            .orElseThrow(() -> new InsufficientAssetException(
                "You do not own " + quote.symbol()
            ));

        if (asset.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientAssetException(
                "Insufficient " + quote.symbol()
                    + " quantity to complete this trade"
            );
        }

        BigDecimal totalAmount = calculateTotal(
            quote.price(),
            quantity
        );

        BigDecimal remainingQuantity = asset.getQuantity()
            .subtract(quantity)
            .setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

        wallet.setCashBalance(
            wallet.getCashBalance()
                .add(totalAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        );

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            assetRepository.delete(asset);
        } else {
            asset.setQuantity(remainingQuantity);
            assetRepository.save(asset);
        }

        TradeTransaction transaction = createTransaction(
            wallet,
            TradeType.SELL,
            quote,
            quantity,
            totalAmount
        );

        transactionRepository.save(transaction);
        tradeSearchIndexer.index(transaction);

        return toTradeResponse(
            transaction,
            wallet,
            remainingQuantity
        );
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(
        String authorizationHeader
    ) {
        Long userId = sessionService.requireUserId(
            authorizationHeader
        );

        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet was not found"
            ));

        List<PortfolioAssetResponse> assets =
            new ArrayList<>();

        BigDecimal cryptoValue = BigDecimal.ZERO
            .setScale(MONEY_SCALE);

        for (
            PortfolioAsset asset :
            assetRepository.findByUserIdOrderBySymbolAsc(userId)
        ) {
            CurrentPriceQuote quote = currentPriceService
                .getCurrentPrice(asset.getSymbol());

            BigDecimal currentValue = quote.price()
                .multiply(asset.getQuantity())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            cryptoValue = cryptoValue.add(currentValue);

            assets.add(new PortfolioAssetResponse(
                asset.getSymbol(),
                asset.getQuantity(),
                quote.price(),
                currentValue
            ));
        }

        BigDecimal totalPortfolioValue = wallet
            .getCashBalance()
            .add(cryptoValue)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new PortfolioResponse(
            userId,
            wallet.getCashBalance(),
            cryptoValue,
            totalPortfolioValue,
            assets
        );
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(
        String authorizationHeader,
        int requestedLimit
    ) {
        Long userId = sessionService.requireUserId(
            authorizationHeader
        );

        int limit = Math.max(
            1,
            Math.min(requestedLimit, 200)
        );

        PageRequest pageRequest = PageRequest.of(
            0,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        );

        return transactionRepository
            .findByUserId(userId, pageRequest)
            .stream()
            .map(transaction -> new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getSymbol(),
                transaction.getQuantity(),
                transaction.getExecutionPrice(),
                transaction.getTotalAmount(),
                transaction.getCreatedAt()
            ))
            .toList();
    }

    private Wallet getWalletForUpdate(Long userId) {
        return walletRepository
            .findByUserIdForUpdate(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Wallet was not found"
            ));
    }

    private BigDecimal normalizeQuantity(
        BigDecimal quantity
    ) {
        if (
            quantity == null
                || quantity.compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new InvalidTradeException(
                "Trade quantity must be greater than zero"
            );
        }

        try {
            return quantity.setScale(
                QUANTITY_SCALE,
                RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new InvalidTradeException(
                "Trade quantity can contain at most 8 decimal places"
            );
        }
    }

    private BigDecimal calculateTotal(
        BigDecimal price,
        BigDecimal quantity
    ) {
        BigDecimal total = price
            .multiply(quantity)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (total.compareTo(new BigDecimal("0.01")) < 0) {
            throw new InvalidTradeException(
                "Trade total must be at least 0.01"
            );
        }

        return total;
    }

    private TradeTransaction createTransaction(
        Wallet wallet,
        TradeType type,
        CurrentPriceQuote quote,
        BigDecimal quantity,
        BigDecimal totalAmount
    ) {
        TradeTransaction transaction =
            new TradeTransaction();

        transaction.setUser(wallet.getUser());
        transaction.setType(type);
        transaction.setSymbol(quote.symbol());
        transaction.setQuantity(quantity);
        transaction.setExecutionPrice(quote.price());
        transaction.setTotalAmount(totalAmount);
        transaction.setCreatedAt(LocalDateTime.now());

        return transaction;
    }

    private TradeResponse toTradeResponse(
        TradeTransaction transaction,
        Wallet wallet,
        BigDecimal assetQuantity
    ) {
        return new TradeResponse(
            transaction.getId(),
            transaction.getType(),
            transaction.getSymbol(),
            transaction.getQuantity(),
            transaction.getExecutionPrice(),
            transaction.getTotalAmount(),
            wallet.getCashBalance(),
            assetQuantity,
            transaction.getCreatedAt()
        );
    }
}