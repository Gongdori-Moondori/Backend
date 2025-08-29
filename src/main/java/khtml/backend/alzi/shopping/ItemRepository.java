package khtml.backend.alzi.shopping;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    
    Optional<Item> findByName(String name);

    boolean existsItemByName(String name);
    
    List<Item> findByNameContainingIgnoreCase(String name);
    
    List<Item> findByCategory(String category);
    
    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.category IS NOT NULL ORDER BY i.category")
    List<String> findAllCategories();

    boolean existsByName(String name);

    // 사용자별 구매 빈도가 높은 아이템 조회
    @Query("SELECT i FROM Item i " +
           "JOIN i.shoppingRecords sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED' " +
           "GROUP BY i " +
           "ORDER BY COUNT(sr) DESC")
    Page<Item> findMostPurchasedItemsByUser(@Param("userId") String userId, Pageable pageable);
    
    // 특정 아이템의 사용자별 구매 횟수
    @Query("SELECT COUNT(sr) FROM ShoppingRecord sr " +
           "JOIN sr.shoppingList sl " +
           "WHERE sr.item.id = :itemId " +
           "AND sl.user.userId = :userId " +
           "AND sr.status = 'PURCHASED'")
    Long countPurchasesByUserAndItem(@Param("userId") String userId, @Param("itemId") Long itemId);
    
    // 카테고리별 아이템 개수 통계
    @Query("SELECT i.category, COUNT(i) FROM Item i WHERE i.category IS NOT NULL GROUP BY i.category ORDER BY COUNT(i) DESC")
    List<Object[]> getCategoryItemCounts();
    
    // 특정 카테고리의 아이템 목록 (이름순)
    @Query("SELECT i.name FROM Item i WHERE i.category = :category ORDER BY i.name")
    List<String> findItemNamesByCategory(@Param("category") String category);
    
    // 전체 아이템 개수
    @Query("SELECT COUNT(i) FROM Item i WHERE i.category IS NOT NULL")
    Long getTotalItemCount();
}
