package khtml.backend.alzi.priceData;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceDataRepository extends JpaRepository<PriceData, String> {
	List<PriceData> findDistinctByMarketName(String marketName);
	
	// 모든 고유한 아이템명 조회
	@Query("SELECT DISTINCT p.itemName FROM PriceData p WHERE p.itemName IS NOT NULL ORDER BY p.itemName")
	List<String> findDistinctItemNames();
	
	// 모든 고유한 마켓명 조회
	@Query("SELECT DISTINCT p.marketName FROM PriceData p WHERE p.marketName IS NOT NULL ORDER BY p.marketName")
	List<String> findDistinctMarketNames();
	
	// marketName과 itemName으로 데이터 조회
	List<PriceData> findByMarketNameAndItemNameOrderByDateDesc(String marketName, String itemName);
	
	// marketName으로만 데이터 조회
	List<PriceData> findByMarketNameOrderByDateDesc(String marketName);
	
	// itemName으로만 데이터 조회
	List<PriceData> findByItemNameOrderByDateDesc(String itemName);
}
