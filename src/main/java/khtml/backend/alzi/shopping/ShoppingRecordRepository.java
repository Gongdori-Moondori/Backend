package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import khtml.backend.alzi.auth.user.User;

public interface ShoppingRecordRepository extends JpaRepository<ShoppingRecord, Long> {
    
    List<ShoppingRecord> findByShoppingListId(Long shoppingListId);
    
    List<ShoppingRecord> findByShoppingListIdAndStatus(Long shoppingListId, ShoppingRecord.PurchaseStatus status);

    // 특정 사용자의 아이템별 구매 기록
    @Query("SELECT sr FROM ShoppingRecord sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId AND sr.item.id = :itemId " +
           "AND sr.status = 'PURCHASED' " +
           "ORDER BY sr.purchasedAt DESC")
    List<ShoppingRecord> findPurchaseHistoryByUserAndItem(@Param("userId") Long userId, @Param("itemId") Long itemId);

    // 사용자별 월 지출액
    @Query("SELECT COALESCE(SUM(sr.price), 0) FROM ShoppingRecord sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED' " +
           "AND YEAR(sr.purchasedAt) = :year AND MONTH(sr.purchasedAt) = :month")
    BigDecimal calculateMonthlyExpense(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    // 카테고리별 지출 통계
    @Query("SELECT sr.item.category, COALESCE(SUM(sr.price), 0) FROM ShoppingRecord sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED' " +
           "AND sr.purchasedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY sr.item.category " +
           "ORDER BY SUM(sr.price) DESC")
    List<Object[]> findExpenseByCategory(@Param("userId") Long userId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // 최근 구매한 아이템들
    @Query("SELECT DISTINCT sr.item FROM ShoppingRecord sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED' " +
           "ORDER BY sr.purchasedAt DESC")
    List<Item> findRecentlyPurchasedItems(@Param("userId") Long userId);
    
    // 특정 ID, 장바구니, 사용자로 ShoppingRecord 조회 (권한 확인용)
    @Query("SELECT sr FROM ShoppingRecord sr " +
           "WHERE sr.id = :recordId " +
           "AND sr.shoppingList = :shoppingList " +
           "AND sr.shoppingList.user = :user")
    Optional<ShoppingRecord> findByIdAndShoppingListAndUser(@Param("recordId") Long recordId, 
                                                           @Param("shoppingList") ShoppingList shoppingList,
                                                           @Param("user") User user);
    
    // 특정 사용자의 모든 구매 완료 기록 조회 (자주 구매한 상품 분석용)
    @Query("SELECT sr FROM ShoppingRecord sr " +
           "JOIN FETCH sr.item " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED' " +
           "ORDER BY sr.purchasedAt DESC")
    List<ShoppingRecord> findPurchasedRecordsByUser(@Param("userId") String userId);
    
    // 특정 장바구니와 아이템으로 ShoppingRecord 조회
    List<ShoppingRecord> findByShoppingListAndItem(ShoppingList shoppingList, Item item);
}
