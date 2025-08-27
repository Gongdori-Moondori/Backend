package khtml.backend.alzi.shopping;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {
    @Query("SELECT sl FROM ShoppingList sl WHERE sl.user.userId = :userId " +
           "AND sl.shoppingDate BETWEEN :startDate AND :endDate " +
           "ORDER BY sl.shoppingDate DESC")
    List<ShoppingList> findByUserIdAndShoppingDateBetween(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    // 월별 쇼핑 리스트 통계
    @Query("SELECT COUNT(sl) FROM ShoppingList sl WHERE sl.user.userId = :userId " +
           "AND YEAR(sl.shoppingDate) = :year AND MONTH(sl.shoppingDate) = :month " +
           "AND sl.status = 'COMPLETED'")
    Long countCompletedShoppingListsByMonth(@Param("userId") Long userId, 
                                           @Param("year") int year, 
                                           @Param("month") int month);
}
