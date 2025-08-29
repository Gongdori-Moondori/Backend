package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import khtml.backend.alzi.auth.user.User;

public interface SavingsRecordRepository extends JpaRepository<SavingsRecord, Long> {
    
    // 사용자별 모든 절약 기록 조회 (최신순)
    List<SavingsRecord> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자별 총 절약 금액 계산
    @Query("SELECT COALESCE(SUM(s.savingsAmount), 0) FROM SavingsRecord s WHERE s.user = :user")
    BigDecimal getTotalSavingsByUser(@Param("user") User user);
    
    // 사용자별 총 절약 횟수
    @Query("SELECT COUNT(s) FROM SavingsRecord s WHERE s.user = :user AND s.savingsAmount > 0")
    Long getTotalSavingsCountByUser(@Param("user") User user);
    
    // 사용자별 총 손해 금액
    @Query("SELECT COALESCE(SUM(ABS(s.savingsAmount)), 0) FROM SavingsRecord s WHERE s.user = :user AND s.savingsAmount < 0")
    BigDecimal getTotalLossByUser(@Param("user") User user);
    
    // 사용자별 특정 기간 절약 금액
    @Query("SELECT COALESCE(SUM(s.savingsAmount), 0) FROM SavingsRecord s WHERE s.user = :user AND s.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getSavingsByUserAndDateRange(@Param("user") User user, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 아이템별 절약 금액 통계 (상위 N개)
    @Query("SELECT s.item.name, SUM(s.savingsAmount) as totalSavings FROM SavingsRecord s WHERE s.user = :user GROUP BY s.item.id, s.item.name ORDER BY totalSavings DESC")
    List<Object[]> getTopSavingsByItem(@Param("user") User user);
    
    // 시장별 절약 금액 통계 (상위 N개)
    @Query("SELECT s.purchasedMarket.name, SUM(s.savingsAmount) as totalSavings FROM SavingsRecord s WHERE s.user = :user GROUP BY s.purchasedMarket.id, s.purchasedMarket.name ORDER BY totalSavings DESC")
    List<Object[]> getTopSavingsByMarket(@Param("user") User user);
    
    // 최근 N일간 절약 금액
    @Query("SELECT COALESCE(SUM(s.savingsAmount), 0) FROM SavingsRecord s WHERE s.user = :user AND s.createdAt >= :fromDate")
    BigDecimal getRecentSavings(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);
    
    // 가장 큰 절약 기록 조회
    @Query("SELECT s FROM SavingsRecord s WHERE s.user = :user ORDER BY s.savingsAmount DESC")
    List<SavingsRecord> getBiggestSavings(@Param("user") User user);
    
    // 특정 쇼핑 리스트의 절약 기록들
    @Query("SELECT s FROM SavingsRecord s WHERE s.shoppingRecord.shoppingList.id = :shoppingListId")
    List<SavingsRecord> findByShoppingListId(@Param("shoppingListId") Long shoppingListId);
    
    // 월별 절약 통계 (최근 12개월)
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, SUM(savings_amount) as totalSavings, COUNT(*) as recordCount " +
                   "FROM savings_record WHERE user_id = :userId AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
                   "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC", nativeQuery = true)
    List<Object[]> getMonthlySavingsStats(@Param("userId") Long userId);
}
