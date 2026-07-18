package com.i2i.cryptopal.portfolio.repository;

import com.i2i.cryptopal.portfolio.entity.PortfolioAsset;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PortfolioAssetRepository
    extends JpaRepository<PortfolioAsset, Long> {

    List<PortfolioAsset> findByUserIdOrderBySymbolAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select asset
        from PortfolioAsset asset
        where asset.user.id = :userId
          and asset.symbol = :symbol
        """)
    Optional<PortfolioAsset> findForUpdate(
        @Param("userId") Long userId,
        @Param("symbol") String symbol
    );
}