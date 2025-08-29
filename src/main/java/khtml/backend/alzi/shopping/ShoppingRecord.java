package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class ShoppingRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shopping_list_id", nullable = false)
	private ShoppingList shoppingList;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "item_id", nullable = false)
	private Item item; // 구매한 물품

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "market_id")
	private khtml.backend.alzi.market.Market market; // 구매한 시장 (새로 추가)

	@Column(nullable = false)
	private Integer quantity; // 수량

	@Column(precision = 10, scale = 2)
	private BigDecimal price; // 가격 (단가 * 수량)

	@Column(precision = 8, scale = 2)
	private BigDecimal unitPrice; // 단가

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PurchaseStatus status; // PLANNED, PURCHASED, CANCELLED

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "purchased_at")
	private LocalDateTime purchasedAt; // 실제 구매한 시간

	@Builder
	public ShoppingRecord(ShoppingList shoppingList, Item item, Integer quantity, BigDecimal unitPrice, khtml.backend.alzi.market.Market market) {
		this.shoppingList = shoppingList;
		this.item = item;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.market = market;
		this.price = unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : null;
		this.status = PurchaseStatus.PLANNED;
		this.createdAt = LocalDateTime.now();
	}

	public void markAsPurchased() {
		this.status = PurchaseStatus.PURCHASED;
		this.purchasedAt = LocalDateTime.now();
	}

	public void cancel() {
		this.status = PurchaseStatus.CANCELLED;
	}

	public void updatePrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
		this.price = unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : null;
	}
	
	public void updateMarket(khtml.backend.alzi.market.Market market) {
		this.market = market;
	}

	public enum PurchaseStatus {
		PLANNED("계획됨"),
		PURCHASED("구매완료"),
		CANCELLED("취소됨");

		private final String description;

		PurchaseStatus(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
