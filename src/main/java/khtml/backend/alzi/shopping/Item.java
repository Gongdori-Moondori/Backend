package khtml.backend.alzi.shopping;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class Item {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name; // 물품명 (예: "사과", "바나나", "우유")

	private String category; // 카테고리 (예: "과일", "유제품", "채소")

	private String description; // 상세 설명

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// 이 물품이 포함된 모든 장보기 기록
	@OneToMany(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Set<ShoppingRecord> shoppingRecords;

	@Builder
	public Item(String name, String category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	public void updateInfo(String category, String description) {
		this.category = category;
		this.description = description;
		this.updatedAt = LocalDateTime.now();
	}
}
