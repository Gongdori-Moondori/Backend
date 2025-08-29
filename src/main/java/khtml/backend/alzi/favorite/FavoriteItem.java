package khtml.backend.alzi.favorite;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.shopping.Item;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
@Table(name = "favorite_items", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id", "market_id"}))
public class FavoriteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 즐겨찾기를 추가한 사용자
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // 즐겨찾기한 아이템
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market; // 즐겨찾기한 시장
    
    @Column(name = "favorite_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal favoritePrice; // 즐겨찾기 등록 시점의 가격
    
    @Column(name = "price_unit")
    private String priceUnit; // 가격 단위 (예: 1kg, 1개 등)
    
    @Column(name = "memo")
    private String memo; // 사용자 메모 (선택사항)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 가격 업데이트 시점
    
    @Builder
    public FavoriteItem(User user, Item item, Market market, BigDecimal favoritePrice, 
                       String priceUnit, String memo) {
        this.user = user;
        this.item = item;
        this.market = market;
        this.favoritePrice = favoritePrice;
        this.priceUnit = priceUnit;
        this.memo = memo;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 즐겨찾기 가격 업데이트
     */
    public void updatePrice(BigDecimal newPrice) {
        this.favoritePrice = newPrice;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 메모 업데이트
     */
    public void updateMemo(String memo) {
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }
}
