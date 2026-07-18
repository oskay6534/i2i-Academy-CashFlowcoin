package com.i2i.cryptopal.portfolio.entity;

import com.i2i.cryptopal.user.entity.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "portfolio_assets",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_portfolio_user_symbol",
            columnNames = {"user_id", "symbol"}
        )
    }
)
public class PortfolioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, precision = 28, scale = 8)
    private BigDecimal quantity;

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}