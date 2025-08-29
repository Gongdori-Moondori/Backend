package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import khtml.backend.alzi.market.Market;

public interface ItemPriceRepository extends JpaRepository<ItemPrice, Long> {
    
    // 특정 아이템과 시장의 가격 정보 조회
    Optional<ItemPrice> findByItemAndMarket(Item item, Market market);
    
    // 특정 아이템의 모든 시장 가격 정보 조회
    List<ItemPrice> findByItem(Item item);
    
    // 특정 시장의 모든 아이템 가격 정보 조회
    List<ItemPrice> findByMarket(Market market);
    
    // 특정 시장명으로 모든 아이템 가격 정보 조회 (날짜 기준 내림차순)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.market.name = :marketName ORDER BY ip.surveyDate DESC, ip.updatedAt DESC")
    List<ItemPrice> findByMarketNameOrderBySurveyDateDesc(@Param("marketName") String marketName);
    
    // 특정 아이템의 최신 가격 정보 조회 (날짜 기준)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.item = :item ORDER BY ip.surveyDate DESC, ip.updatedAt DESC")
    List<ItemPrice> findByItemOrderByDateDesc(@Param("item") Item item);
    
    // 특정 아이템의 특정 날짜 가격 정보 조회
    List<ItemPrice> findByItemAndSurveyDate(Item item, LocalDate surveyDate);
    
    // 특정 아이템의 평균 가격 계산
    @Query("SELECT AVG(ip.price) FROM ItemPrice ip WHERE ip.item = :item")
    Double findAveragePriceByItem(@Param("item") Item item);
    
    // 특정 아이템의 최저 가격 조회
    @Query("SELECT MIN(ip.price) FROM ItemPrice ip WHERE ip.item = :item")
    BigDecimal findMinPriceByItem(@Param("item") Item item);
    
    // 특정 아이템의 최고 가격 조회
    @Query("SELECT MAX(ip.price) FROM ItemPrice ip WHERE ip.item = :item")
    BigDecimal findMaxPriceByItem(@Param("item") Item item);
    
    // 특정 기간 내 가격 정보 조회
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.surveyDate BETWEEN :startDate AND :endDate")
    List<ItemPrice> findBySurveyDateBetween(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
    
    // 특정 아이템, 시장, 날짜로 가격 정보 조회 (API 업데이트용)
    Optional<ItemPrice> findByItemAndMarketAndSurveyDate(Item item, Market market, LocalDate surveyDate);
    
    // 아이템명으로 모든 가격 정보 조회 (절약 분석용, 0원 제외)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.item.name = :itemName AND ip.price > 0 ORDER BY ip.surveyDate DESC")
    List<ItemPrice> findAllByItemName(@Param("itemName") String itemName);
    
    // 특정 마트와 아이템명으로 가격 정보 조회 (시장 vs 마트 비교용, 0원 제외)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.market.name = :marketName AND ip.item.name = :itemName AND ip.price > 0 ORDER BY ip.surveyDate DESC, ip.updatedAt DESC")
    List<ItemPrice> findByMarketNameAndItemName(@Param("marketName") String marketName, @Param("itemName") String itemName);
    
    // 특정 아이템의 모든 시장 가격 정보 조회 (0원 제외)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.item = :item AND ip.price > 0 ORDER BY ip.price ASC")
    List<ItemPrice> findByItemExcludingZeroPrice(@Param("item") Item item);
    
    // 특정 시장의 모든 아이템 가격 정보 조회 (0원 제외)
    @Query("SELECT ip FROM ItemPrice ip WHERE ip.market = :market AND ip.price > 0 ORDER BY ip.item.name ASC")
    List<ItemPrice> findByMarketExcludingZeroPrice(@Param("market") Market market);
    
    // 특정 아이템의 평균 가격 계산 (0원 제외)
    @Query("SELECT AVG(ip.price) FROM ItemPrice ip WHERE ip.item = :item AND ip.price > 0")
    Double findAveragePriceByItemExcludingZero(@Param("item") Item item);
    
    // 특정 아이템의 최저 가격 조회 (0원 제외)
    @Query("SELECT MIN(ip.price) FROM ItemPrice ip WHERE ip.item = :item AND ip.price > 0")
    BigDecimal findMinPriceByItemExcludingZero(@Param("item") Item item);
    
    // 특정 아이템의 최고 가격 조회 (0원 제외)
    @Query("SELECT MAX(ip.price) FROM ItemPrice ip WHERE ip.item = :item AND ip.price > 0")
    BigDecimal findMaxPriceByItemExcludingZero(@Param("item") Item item);
}
