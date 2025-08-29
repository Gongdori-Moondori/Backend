package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.market.Market;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class SavingsRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 절약한 사용자
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_record_id", nullable = false)
    private ShoppingRecord shoppingRecord; // 구매 기록 참조
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // 구매한 아이템
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_market_id", nullable = false)
    private Market purchasedMarket; // 실제 구매한 시장
    
    @Column(name = "purchased_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal purchasedPrice; // 실제 구매한 가격
    
    @Column(name = "comparison_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal comparisonPrice; // 비교 대상 가격 (대형마트 평균 등)
    
    @Column(name = "savings_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal savingsAmount; // 절약한 금액 (comparisonPrice - purchasedPrice)
    
    @Column(name = "comparison_type", nullable = false)
    private String comparisonType; // 비교 기준 ("LARGE_MART_AVERAGE", "MARKET_AVERAGE", "HIGHEST_PRICE")
    
    @Column(name = "comparison_market_names")
    private String comparisonMarketNames; // 비교한 시장명들 (쉼표로 구분)
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity; // 구매 수량
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Builder
    public SavingsRecord(User user, ShoppingRecord shoppingRecord, Item item, Market purchasedMarket,
                        BigDecimal purchasedPrice, BigDecimal comparisonPrice, BigDecimal savingsAmount,
                        String comparisonType, String comparisonMarketNames, Integer quantity) {
        this.user = user;
        this.shoppingRecord = shoppingRecord;
        this.item = item;
        this.purchasedMarket = purchasedMarket;
        this.purchasedPrice = purchasedPrice;
        this.comparisonPrice = comparisonPrice;
        this.savingsAmount = savingsAmount;
        this.comparisonType = comparisonType;
        this.comparisonMarketNames = comparisonMarketNames;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 절약 비율 계산 (%)
     */
    public double getSavingsPercentage() {
        if (comparisonPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return savingsAmount.divide(comparisonPrice, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
    
    /**
     * 손해 여부 확인
     */
    public boolean isLoss() {
        return savingsAmount.compareTo(BigDecimal.ZERO) < 0;
    }
}
