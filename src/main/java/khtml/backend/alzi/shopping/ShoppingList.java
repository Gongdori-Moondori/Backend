package khtml.backend.alzi.shopping;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import khtml.backend.alzi.auth.user.User;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class ShoppingList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title; // 장보기 리스트 제목 (예: "주간 장보기", "파티 준비")
    
    private String description; // 설명
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingListStatus status; // PLANNED, IN_PROGRESS, COMPLETED
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user", nullable = false)
    private User user; // 장보기 리스트 소유자
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at") 
    private LocalDateTime updatedAt;
    
    @Column(name = "shopping_date")
    private LocalDateTime shoppingDate; // 실제 장본 날짜
    
    // 이 리스트에 포함된 모든 쇼핑 기록
    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShoppingRecord> shoppingRecords;
    
    @Builder
    public ShoppingList(String title, String description, User user, LocalDateTime shoppingDate) {
        this.title = title;
        this.description = description;
        this.user = user;
        this.status = ShoppingListStatus.PLANNED;
        this.shoppingDate = shoppingDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateStatus(ShoppingListStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        
        if (status == ShoppingListStatus.COMPLETED && this.shoppingDate == null) {
            this.shoppingDate = LocalDateTime.now();
        }
    }
    
    public enum ShoppingListStatus {
        PLANNED("계획됨"),
        IN_PROGRESS("진행중"),
        COMPLETED("완료됨");
        
        private final String description;
        
        ShoppingListStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
