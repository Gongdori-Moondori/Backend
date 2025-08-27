package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import khtml.backend.alzi.market.Market;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class ItemPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // 아이템
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_code", referencedColumnName = "code", nullable = false)
    private Market market; // 시장 (code로 참조)
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // 가격
    
    @Column(name = "price_unit")
    private String priceUnit; // 가격 단위 (예: "개당", "kg당")
    
    @Column(name = "survey_date")
    private LocalDate surveyDate; // 조사 날짜
    
    @Column(name = "additional_info")
    private String additionalInfo; // 추가 정보 (예: "5개10000")
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Builder
    public ItemPrice(Item item, Market market, BigDecimal price, String priceUnit, 
                    LocalDate surveyDate, String additionalInfo) {
        this.item = item;
        this.market = market;
        this.price = price;
        this.priceUnit = priceUnit;
        this.surveyDate = surveyDate;
        this.additionalInfo = additionalInfo;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updatePrice(BigDecimal price, String priceUnit, LocalDate surveyDate, String additionalInfo) {
        this.price = price;
        this.priceUnit = priceUnit;
        this.surveyDate = surveyDate;
        this.additionalInfo = additionalInfo;
        this.updatedAt = LocalDateTime.now();
    }
}
