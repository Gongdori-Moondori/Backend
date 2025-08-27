package khtml.backend.alzi.shopping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import khtml.backend.alzi.auth.user.User;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    
    // 사용자별 장보기 리스트 조회 (최신순)
    List<ShoppingList> findByUserOrderByCreatedAtDesc(User user);
    
    // 특정 사용자의 특정 장보기 리스트 조회
    Optional<ShoppingList> findByIdAndUser(Long id, User user);
    
    // 사용자별 상태별 장보기 리스트 조회
    List<ShoppingList> findByUserAndStatusOrderByCreatedAtDesc(User user, ShoppingList.ShoppingListStatus status);
}
