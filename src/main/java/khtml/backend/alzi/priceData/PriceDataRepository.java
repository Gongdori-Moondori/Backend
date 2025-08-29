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
	
	// marketName과 itemName으로 데이터 조회 (0원 제외)
	@Query("SELECT p FROM PriceData p WHERE p.marketName = :marketName AND p.itemName = :itemName " +
		   "AND p.price IS NOT NULL AND p.price != '' AND p.price != '0' AND p.price NOT LIKE '0원' " +
		   "AND p.price NOT LIKE '0%' ORDER BY p.date DESC")
	List<PriceData> findByMarketNameAndItemNameOrderByDateDesc(@Param("marketName") String marketName, @Param("itemName") String itemName);
	
	// marketName으로만 데이터 조회 (0원 제외)
	@Query("SELECT p FROM PriceData p WHERE p.marketName = :marketName " +
		   "AND p.price IS NOT NULL AND p.price != '' AND p.price != '0' AND p.price NOT LIKE '0원' " +
		   "AND p.price NOT LIKE '0%' ORDER BY p.date DESC")
	List<PriceData> findByMarketNameOrderByDateDesc(@Param("marketName") String marketName);
	
	// itemName으로만 데이터 조회 (0원 제외)
	@Query("SELECT p FROM PriceData p WHERE p.itemName = :itemName " +
		   "AND p.price IS NOT NULL AND p.price != '' AND p.price != '0' AND p.price NOT LIKE '0원' " +
		   "AND p.price NOT LIKE '0%' ORDER BY p.date DESC")
	List<PriceData> findByItemNameOrderByDateDesc(@Param("itemName") String itemName);
}
