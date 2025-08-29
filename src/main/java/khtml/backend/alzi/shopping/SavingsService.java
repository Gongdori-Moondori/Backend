package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.market.MarketRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsService {
    
    private final SavingsRecordRepository savingsRecordRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final MarketRepository marketRepository;
    
    // 대형마트 목록 (확장 가능)
    private static final List<String> LARGE_MART_NAMES = Arrays.asList(
        "이마트", "롯데마트", "홈플러스", "코스트코", "하나로마트"
    );
    
    /**
     * 쇼핑 완료 시 절약 금액 계산 및 저장
     */
    @Transactional
    public SavingsCalculationResult calculateAndSaveSavings(ShoppingRecord shoppingRecord, User user) {
        log.info("절약 금액 계산 시작 - 사용자: {}, 아이템: {}", 
                user.getUserId(), shoppingRecord.getItem().getName());
        
        try {
            // 1. 실제 구매 정보
            Item purchasedItem = shoppingRecord.getItem();
            BigDecimal purchasedUnitPrice = shoppingRecord.getUnitPrice();
            Integer quantity = shoppingRecord.getQuantity();
            BigDecimal totalPurchasedPrice = purchasedUnitPrice.multiply(BigDecimal.valueOf(quantity));
            
            // 2. 구매한 시장 정보 (ShoppingRecord에 시장 정보가 없다면 추가 필요)
            Market purchasedMarket = findPurchasedMarket(shoppingRecord);
            if (purchasedMarket == null) {
                log.warn("구매 시장 정보를 찾을 수 없습니다 - ShoppingRecord ID: {}", shoppingRecord.getId());
                return SavingsCalculationResult.noComparison("구매 시장 정보 없음");
            }
            
            // 3. 비교 가격 계산 (대형마트 평균)
            ComparisonPriceResult comparisonResult = calculateComparisonPrice(purchasedItem, quantity);
            
            if (!comparisonResult.isValid()) {
                log.info("비교 가격 정보가 충분하지 않습니다 - 아이템: {}", purchasedItem.getName());
                return SavingsCalculationResult.noComparison("비교 가격 정보 부족");
            }
            
            // 4. 절약 금액 계산
            BigDecimal savingsAmount = comparisonResult.getTotalPrice().subtract(totalPurchasedPrice);
            
            // 5. 절약 기록 저장
            SavingsRecord savingsRecord = SavingsRecord.builder()
                    .user(user)
                    .shoppingRecord(shoppingRecord)
                    .item(purchasedItem)
                    .purchasedMarket(purchasedMarket)
                    .purchasedPrice(totalPurchasedPrice)
                    .comparisonPrice(comparisonResult.getTotalPrice())
                    .savingsAmount(savingsAmount)
                    .comparisonType(comparisonResult.getComparisonType())
                    .comparisonMarketNames(comparisonResult.getComparisonMarketNames())
                    .quantity(quantity)
                    .build();
            
            savingsRecordRepository.save(savingsRecord);
            
            log.info("절약 기록 저장 완료 - 절약 금액: {}원", savingsAmount);
            
            return SavingsCalculationResult.success(savingsRecord, comparisonResult);
            
        } catch (Exception e) {
            log.error("절약 금액 계산 중 오류 발생", e);
            return SavingsCalculationResult.error("계산 중 오류 발생: " + e.getMessage());
        }
    }
    
    /**
     * 비교 가격 계산 (대형마트 평균)
     */
    private ComparisonPriceResult calculateComparisonPrice(Item item, Integer quantity) {
        
        // 대형마트에서의 가격 정보 조회
        List<ItemPrice> largeMartPrices = itemPriceRepository.findAllByItemName(item.getName())
                .stream()
                .filter(ip -> LARGE_MART_NAMES.contains(ip.getMarket().getName()))
                .filter(ip -> ip.getPrice() != null && ip.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        if (largeMartPrices.isEmpty()) {
            // 대형마트 가격이 없으면 전체 시장 평균 사용
            return calculateAllMarketAveragePrice(item, quantity);
        }
        
        // 대형마트 평균 가격 계산
        BigDecimal totalPrice = largeMartPrices.stream()
                .map(ItemPrice::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageUnitPrice = totalPrice.divide(
                BigDecimal.valueOf(largeMartPrices.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalAveragePrice = averageUnitPrice.multiply(BigDecimal.valueOf(quantity));
        
        String marketNames = largeMartPrices.stream()
                .map(ip -> ip.getMarket().getName())
                .distinct()
                .collect(Collectors.joining(", "));
        
        return ComparisonPriceResult.builder()
                .totalPrice(totalAveragePrice)
                .unitPrice(averageUnitPrice)
                .comparisonType("LARGE_MART_AVERAGE")
                .comparisonMarketNames(marketNames)
                .comparisonCount(largeMartPrices.size())
                .valid(true)
                .build();
    }
    
    /**
     * 전체 시장 평균 가격 계산 (대형마트 가격이 없을 때)
     */
    private ComparisonPriceResult calculateAllMarketAveragePrice(Item item, Integer quantity) {
        
        List<ItemPrice> allPrices = itemPriceRepository.findAllByItemName(item.getName())
                .stream()
                .filter(ip -> ip.getPrice() != null && ip.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        if (allPrices.size() < 2) { // 비교할 시장이 너무 적으면
            return ComparisonPriceResult.invalid();
        }
        
        BigDecimal totalPrice = allPrices.stream()
                .map(ItemPrice::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageUnitPrice = totalPrice.divide(
                BigDecimal.valueOf(allPrices.size()), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalAveragePrice = averageUnitPrice.multiply(BigDecimal.valueOf(quantity));
        
        String marketNames = allPrices.stream()
                .map(ip -> ip.getMarket().getName())
                .distinct()
                .limit(5) // 최대 5개 시장명만 표시
                .collect(Collectors.joining(", "));
        
        return ComparisonPriceResult.builder()
                .totalPrice(totalAveragePrice)
                .unitPrice(averageUnitPrice)
                .comparisonType("ALL_MARKET_AVERAGE")
                .comparisonMarketNames(marketNames)
                .comparisonCount(allPrices.size())
                .valid(true)
                .build();
    }
    
    /**
     * 구매한 시장 정보 찾기
     */
    private Market findPurchasedMarket(ShoppingRecord shoppingRecord) {
        // ShoppingRecord에서 직접 시장 정보 가져오기
        if (shoppingRecord.getMarket() != null) {
            return shoppingRecord.getMarket();
        }
        
        // 시장 정보가 없으면 기본 시장으로 설정 (임시 처리)
        log.warn("ShoppingRecord ID {}에 시장 정보가 없습니다. 기본 시장으로 설정합니다.", shoppingRecord.getId());
        Optional<Market> defaultMarket = marketRepository.findByName("경동시장");
        return defaultMarket.orElse(null);
    }
    
    /**
     * 사용자 절약 통계 조회
     */
    @Transactional(readOnly = true)
    public UserSavingsStats getUserSavingsStats(User user) {
        BigDecimal totalSavings = savingsRecordRepository.getTotalSavingsByUser(user);
        Long savingsCount = savingsRecordRepository.getTotalSavingsCountByUser(user);
        BigDecimal totalLoss = savingsRecordRepository.getTotalLossByUser(user);
        
        // 최근 30일 절약 금액
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal recentSavings = savingsRecordRepository.getRecentSavings(user, thirtyDaysAgo);
        
        // 아이템별 절약 TOP 5
        List<Object[]> topItemSavings = savingsRecordRepository.getTopSavingsByItem(user);
        List<ItemSavingsInfo> topItems = topItemSavings.stream()
                .limit(5)
                .map(row -> new ItemSavingsInfo((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
        
        // 시장별 절약 TOP 5
        List<Object[]> topMarketSavings = savingsRecordRepository.getTopSavingsByMarket(user);
        List<MarketSavingsInfo> topMarkets = topMarketSavings.stream()
                .limit(5)
                .map(row -> new MarketSavingsInfo((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
        
        return UserSavingsStats.builder()
                .totalSavings(totalSavings != null ? totalSavings : BigDecimal.ZERO)
                .totalSavingsCount(savingsCount != null ? savingsCount : 0L)
                .totalLoss(totalLoss != null ? totalLoss : BigDecimal.ZERO)
                .recentThirtyDaysSavings(recentSavings != null ? recentSavings : BigDecimal.ZERO)
                .topItemSavings(topItems)
                .topMarketSavings(topMarkets)
                .build();
    }
    
    // === DTO Classes ===
    
    @Data
    @lombok.Builder
    public static class SavingsCalculationResult {
        private boolean success;
        private SavingsRecord savingsRecord;
        private ComparisonPriceResult comparisonResult;
        private String message;
        
        public static SavingsCalculationResult success(SavingsRecord savingsRecord, ComparisonPriceResult comparisonResult) {
            return SavingsCalculationResult.builder()
                    .success(true)
                    .savingsRecord(savingsRecord)
                    .comparisonResult(comparisonResult)
                    .message("절약 금액 계산 완료")
                    .build();
        }
        
        public static SavingsCalculationResult noComparison(String reason) {
            return SavingsCalculationResult.builder()
                    .success(false)
                    .message("비교 불가: " + reason)
                    .build();
        }
        
        public static SavingsCalculationResult error(String message) {
            return SavingsCalculationResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
    
    @Data
    @lombok.Builder
    public static class ComparisonPriceResult {
        private BigDecimal totalPrice;
        private BigDecimal unitPrice;
        private String comparisonType;
        private String comparisonMarketNames;
        private int comparisonCount;
        private boolean valid;
        
        public static ComparisonPriceResult invalid() {
            return ComparisonPriceResult.builder()
                    .valid(false)
                    .build();
        }
    }
    
    @Data
    @lombok.Builder
    public static class UserSavingsStats {
        private BigDecimal totalSavings;
        private Long totalSavingsCount;
        private BigDecimal totalLoss;
        private BigDecimal recentThirtyDaysSavings;
        private List<ItemSavingsInfo> topItemSavings;
        private List<MarketSavingsInfo> topMarketSavings;
    }
    
    @Data
    @lombok.AllArgsConstructor
    public static class ItemSavingsInfo {
        private String itemName;
        private BigDecimal totalSavings;
    }
    
    @Data
    @lombok.AllArgsConstructor
    public static class MarketSavingsInfo {
        private String marketName;
        private BigDecimal totalSavings;
    }
}
