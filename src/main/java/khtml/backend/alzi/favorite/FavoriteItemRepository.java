package khtml.backend.alzi.favorite;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.shopping.Item;

public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    
    // 사용자별 즐겨찾기 목록 조회 (최신순)
    List<FavoriteItem> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자별 즐겨찾기 개수
    Long countByUser(User user);
    
    // 특정 사용자의 특정 아이템-시장 조합 조회
    Optional<FavoriteItem> findByUserAndItemAndMarket(User user, Item item, Market market);
    
    // 특정 사용자의 특정 아이템 즐겨찾기 목록 (모든 시장)
    List<FavoriteItem> findByUserAndItem(User user, Item item);
    
    // 특정 사용자의 특정 시장 즐겨찾기 목록
    List<FavoriteItem> findByUserAndMarket(User user, Market market);
    
    // 즐겨찾기 존재 여부 확인
    boolean existsByUserAndItemAndMarket(User user, Item item, Market market);
    
    // 사용자별 즐겨찾기 총 가격 합계
    @Query("SELECT COALESCE(SUM(f.favoritePrice), 0) FROM FavoriteItem f WHERE f.user = :user")
    BigDecimal getTotalFavoritePriceByUser(@Param("user") User user);
    
    // 시장별 즐겨찾기 개수 통계
    @Query("SELECT f.market.name, COUNT(f) FROM FavoriteItem f WHERE f.user = :user GROUP BY f.market.id, f.market.name ORDER BY COUNT(f) DESC")
    List<Object[]> getFavoriteCountByMarket(@Param("user") User user);
    
    // 아이템별 즐겨찾기 개수 통계
    @Query("SELECT f.item.name, COUNT(f) FROM FavoriteItem f WHERE f.user = :user GROUP BY f.item.id, f.item.name ORDER BY COUNT(f) DESC")
    List<Object[]> getFavoriteCountByItem(@Param("user") User user);
    
    // 최근 추가된 즐겨찾기 (상위 N개)
    @Query("SELECT f FROM FavoriteItem f WHERE f.user = :user ORDER BY f.createdAt DESC")
    List<FavoriteItem> findRecentFavoritesByUser(@Param("user") User user);
    
    // 특정 기간에 추가된 즐겨찾기
    @Query("SELECT f FROM FavoriteItem f WHERE f.user = :user AND f.createdAt >= :fromDate ORDER BY f.createdAt DESC")
    List<FavoriteItem> findFavoritesByUserSinceDate(@Param("user") User user, @Param("fromDate") java.time.LocalDateTime fromDate);
}
