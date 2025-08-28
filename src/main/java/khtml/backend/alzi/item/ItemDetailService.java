package khtml.backend.alzi.item;

import khtml.backend.alzi.market.dto.response.MarketItemPriceResponse;
import khtml.backend.alzi.priceData.PriceData;
import khtml.backend.alzi.priceData.PriceDataRepository;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import khtml.backend.alzi.shopping.ItemRepository;
import khtml.backend.alzi.utils.PricePredictionUtil;
import khtml.backend.alzi.utils.SeasonalRecommendationUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemDetailService {

    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final PriceDataRepository priceDataRepository;
    private final PricePredictionUtil pricePredictionUtil;
    private final SeasonalRecommendationUtil seasonalRecommendationUtil;

    @Data
    public static class ItemDetailResponse {
        private String itemName;
        private String category;
        private BigDecimal currentAveragePrice;     // 현재 평균 가격
        private String priceUnit;                   // 가격 단위

        // 월별 가격 정보
        private List<MonthlyPriceInfo> monthlyPrices;

        // 가격 예측 정보
        private PricePredictionUtil.PriceAnalysis priceAnalysis;

        // 시장별 현재 가격 정보
        private List<MarketPriceInfo> marketPrices;

        // 계절성 정보
        private boolean isSeasonalItem;             // 현재 제철 여부
        private List<String> seasonalMonths;        // 제철 월들
        private List<String> benefits;              // 영양/건강 효능

        // 통계 정보
        private ItemStatistics statistics;
    }

    @Data
    public static class MarketItemDetailResponse {
        private String itemName;
        private String category;
        private String marketName;

        // 해당 시장에서의 가격 정보
        private BigDecimal currentPrice;            // 현재 가격
        private String priceUnit;                   // 가격 단위
        private LocalDate lastUpdated;              // 마지막 업데이트

        // 해당 시장에서의 월별 가격 변화
        private List<MonthlyPriceInfo> monthlyPrices;

        // 다른 시장과의 비교
        private List<MarketComparisonInfo> marketComparisons;

        // 가격 예측 (해당 시장 기준)
        private PricePredictionUtil.PriceAnalysis priceAnalysis;

        // 추가 정보
        private boolean isSeasonalItem;
        private String recommendation;              // 구매 추천
        private String comparisonSummary;           // 비교 요약
    }

    @Data
    public static class MonthlyPriceInfo {
        private String month;                       // 월 이름 (1월, 2월...)
        private int monthNumber;                    // 월 숫자 (1, 2...)
        private BigDecimal averagePrice;            // 해당 월 평균 가격
        private BigDecimal minPrice;                // 해당 월 최저 가격
        private BigDecimal maxPrice;                // 해당 월 최고 가격
        private int dataPointsCount;                // 데이터 개수
        private boolean isCurrentMonth;             // 현재 월 여부
        private boolean isSeasonalPeak;             // 제철 성수기 여부
    }

    @Data
    public static class MarketPriceInfo {
        private String marketName;
        private BigDecimal price;
        private String priceUnit;
        private LocalDate lastUpdated;
        private String marketType;                  // "TRADITIONAL" or "LARGE_MART"
        private boolean isAvailable;
    }

    @Data
    public static class MarketComparisonInfo {
        private String marketName;
        private BigDecimal price;
        private BigDecimal priceDifference;         // 현재 시장과의 차이
        private double percentageDifference;        // 비율 차이
        private String comparison;                  // "더 저렴", "더 비쌈", "비슷함"
        private String marketType;
    }

    @Data
    public static class ItemStatistics {
        private BigDecimal overallAveragePrice;     // 전체 평균 가격
        private BigDecimal overallMinPrice;         // 전체 최저 가격
        private BigDecimal overallMaxPrice;         // 전체 최고 가격
        private double volatility;                  // 변동성
        private int totalMarkets;                   // 판매 시장 수
        private int totalDataPoints;                // 전체 데이터 포인트 수
        private String mostExpensiveMarket;         // 가장 비싼 시장
        private String cheapestMarket;              // 가장 저렴한 시장
    }

    /**
     * 아이템 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public ItemDetailResponse getItemDetail(String itemName) {
        log.info("아이템 '{}' 상세 정보 조회 시작", itemName);

        try {
            // 1. 아이템 기본 정보 조회
            Optional<Item> itemOpt = itemRepository.findByName(itemName);
            if (itemOpt.isEmpty()) {
                log.warn("아이템 '{}'를 찾을 수 없습니다", itemName);
                return null;
            }

            Item item = itemOpt.get();
            ItemDetailResponse response = new ItemDetailResponse();
            response.setItemName(item.getName());
            response.setCategory(item.getCategory());

            // 2. 현재 평균 가격 계산
            List<ItemPrice> allItemPrices = itemPriceRepository.findAllByItemName(itemName);
            BigDecimal currentAveragePrice = calculateCurrentAveragePrice(allItemPrices);
            response.setCurrentAveragePrice(currentAveragePrice);
            response.setPriceUnit(getCommonPriceUnit(allItemPrices));

            // 3. 월별 가격 정보 생성
            List<MonthlyPriceInfo> monthlyPrices = generateMonthlyPriceInfo(itemName);
            response.setMonthlyPrices(monthlyPrices);

            // 4. 가격 예측 분석
            PricePredictionUtil.PriceAnalysis priceAnalysis = generatePriceAnalysis(itemName);
            response.setPriceAnalysis(priceAnalysis);

            // 5. 시장별 현재 가격 정보
            List<MarketPriceInfo> marketPrices = generateMarketPriceInfo(allItemPrices);
            response.setMarketPrices(marketPrices);

            // 6. 계절성 정보
            boolean isSeasonalItem = seasonalRecommendationUtil.isCurrentlyInSeason(itemName);
            response.setSeasonalItem(isSeasonalItem);
            
            List<String> seasonalMonths = seasonalRecommendationUtil.getSeasonalMonths(itemName)
                    .stream()
                    .map(month -> month.getDisplayName(TextStyle.FULL, Locale.KOREAN))
                    .collect(Collectors.toList());
            response.setSeasonalMonths(seasonalMonths);

            // 7. 통계 정보
            ItemStatistics statistics = generateItemStatistics(allItemPrices);
            response.setStatistics(statistics);

            log.info("아이템 '{}' 상세 정보 조회 완료", itemName);
            return response;

        } catch (Exception e) {
            log.error("아이템 '{}' 상세 정보 조회 실패: {}", itemName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 특정 시장에서의 아이템 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public MarketItemDetailResponse getMarketItemDetail(String itemName, String marketName) {
        log.info("시장 '{}' 아이템 '{}' 상세 정보 조회 시작", marketName, itemName);

        try {
            // 1. 해당 시장-아이템 조합 확인
            List<ItemPrice> marketItemPrices = itemPriceRepository
                    .findByMarketNameAndItemName(marketName, itemName);
            
            if (marketItemPrices.isEmpty()) {
                log.warn("시장 '{}'에서 아이템 '{}'을 찾을 수 없습니다", marketName, itemName);
                return null;
            }

            MarketItemDetailResponse response = new MarketItemDetailResponse();
            response.setItemName(itemName);
            response.setMarketName(marketName);

            // 2. 아이템 카테고리 설정
            Optional<Item> itemOpt = itemRepository.findByName(itemName);
            if (itemOpt.isPresent()) {
                response.setCategory(itemOpt.get().getCategory());
            }

            // 3. 현재 가격 정보
            ItemPrice latestPrice = marketItemPrices.get(0);
            response.setCurrentPrice(latestPrice.getPrice());
            response.setPriceUnit(latestPrice.getPriceUnit());
            response.setLastUpdated(latestPrice.getSurveyDate());

            // 4. 해당 시장에서의 월별 가격 변화
            List<MonthlyPriceInfo> monthlyPrices = generateMarketMonthlyPriceInfo(itemName, marketName);
            response.setMonthlyPrices(monthlyPrices);

            // 5. 다른 시장과의 비교
            List<MarketComparisonInfo> marketComparisons = generateMarketComparisons(itemName, marketName, latestPrice.getPrice());
            response.setMarketComparisons(marketComparisons);

            // 6. 가격 예측 (해당 시장 기준)
            PricePredictionUtil.PriceAnalysis priceAnalysis = generateMarketPriceAnalysis(itemName, marketName);
            response.setPriceAnalysis(priceAnalysis);

            // 7. 추가 정보
            response.setSeasonalItem(seasonalRecommendationUtil.isCurrentlyInSeason(itemName));
            response.setRecommendation(generatePurchaseRecommendation(latestPrice.getPrice(), marketComparisons));
            response.setComparisonSummary(generateComparisonSummary(marketName, marketComparisons));

            log.info("시장 '{}' 아이템 '{}' 상세 정보 조회 완료", marketName, itemName);
            return response;

        } catch (Exception e) {
            log.error("시장 '{}' 아이템 '{}' 상세 정보 조회 실패: {}", marketName, itemName, e.getMessage(), e);
            return null;
        }
    }

    // === Private Helper Methods ===

    private BigDecimal calculateCurrentAveragePrice(List<ItemPrice> itemPrices) {
        if (itemPrices.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> validPrices = itemPrices.stream()
                .map(ItemPrice::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        if (validPrices.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = validPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(validPrices.size()), 2, RoundingMode.HALF_UP);
    }

    private String getCommonPriceUnit(List<ItemPrice> itemPrices) {
        return itemPrices.stream()
                .map(ItemPrice::getPriceUnit)
                .filter(unit -> unit != null && !unit.trim().isEmpty())
                .findFirst()
                .orElse("1개");
    }

    private List<MonthlyPriceInfo> generateMonthlyPriceInfo(String itemName) {
        // 과거 12개월 데이터 조회
        List<PriceData> priceDataList = priceDataRepository.findByItemNameOrderByDateDesc(itemName);
        
        // 월별로 그룹화
        Map<Month, List<PriceData>> monthlyData = priceDataList.stream()
                .filter(pd -> pd.getDate() != null)
                .collect(Collectors.groupingBy(pd -> pd.getDate().getMonth()));

        return Arrays.stream(Month.values())
                .map(month -> {
                    List<PriceData> monthData = monthlyData.getOrDefault(month, new ArrayList<>());
                    
                    MonthlyPriceInfo monthlyInfo = new MonthlyPriceInfo();
                    monthlyInfo.setMonth(month.getDisplayName(TextStyle.FULL, Locale.KOREAN));
                    monthlyInfo.setMonthNumber(month.getValue());
                    monthlyInfo.setCurrentMonth(month == LocalDate.now().getMonth());
                    monthlyInfo.setDataPointsCount(monthData.size());
                    
                    if (!monthData.isEmpty()) {
                        List<BigDecimal> prices = monthData.stream()
                                .map(pd -> {
                                    try {
                                        return new BigDecimal(pd.getPrice().replaceAll("[^0-9.]", ""));
                                    } catch (Exception e) {
                                        return BigDecimal.ZERO;
                                    }
                                })
                                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                                .collect(Collectors.toList());

                        if (!prices.isEmpty()) {
                            BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                            monthlyInfo.setAveragePrice(sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
                            monthlyInfo.setMinPrice(prices.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
                            monthlyInfo.setMaxPrice(prices.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
                        }
                    }
                    
                    // 제철 성수기 여부
                    monthlyInfo.setSeasonalPeak(seasonalRecommendationUtil.getSeasonalMonths(itemName).contains(month));
                    
                    return monthlyInfo;
                })
                .collect(Collectors.toList());
    }

    private PricePredictionUtil.PriceAnalysis generatePriceAnalysis(String itemName) {
        try {
            List<PriceData> priceDataList = priceDataRepository.findByItemNameOrderByDateDesc(itemName);
            
            List<MarketItemPriceResponse.PriceDataInfo> priceDataInfos = priceDataList.stream()
                    .map(MarketItemPriceResponse.PriceDataInfo::from)
                    .collect(Collectors.toList());

            return pricePredictionUtil.analyzePriceHistory(itemName, priceDataInfos);
            
        } catch (Exception e) {
            log.debug("아이템 '{}' 가격 예측 분석 실패: {}", itemName, e.getMessage());
            return null;
        }
    }

    private List<MarketPriceInfo> generateMarketPriceInfo(List<ItemPrice> allItemPrices) {
        List<String> largeMarts = List.of("이마트", "롯데마트", "홈플러스");
        
        return allItemPrices.stream()
                .collect(Collectors.groupingBy(ip -> ip.getMarket().getName()))
                .entrySet().stream()
                .map(entry -> {
                    String marketName = entry.getKey();
                    List<ItemPrice> marketPrices = entry.getValue();
                    ItemPrice latestPrice = marketPrices.get(0);

                    MarketPriceInfo marketInfo = new MarketPriceInfo();
                    marketInfo.setMarketName(marketName);
                    marketInfo.setPrice(latestPrice.getPrice());
                    marketInfo.setPriceUnit(latestPrice.getPriceUnit());
                    marketInfo.setLastUpdated(latestPrice.getSurveyDate());
                    marketInfo.setMarketType(largeMarts.contains(marketName) ? "LARGE_MART" : "TRADITIONAL");
                    marketInfo.setAvailable(true);

                    return marketInfo;
                })
                .sorted(Comparator.comparing(MarketPriceInfo::getPrice))
                .collect(Collectors.toList());
    }

    private ItemStatistics generateItemStatistics(List<ItemPrice> allItemPrices) {
        if (allItemPrices.isEmpty()) return new ItemStatistics();

        List<BigDecimal> validPrices = allItemPrices.stream()
                .map(ItemPrice::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        ItemStatistics stats = new ItemStatistics();
        stats.setTotalDataPoints(allItemPrices.size());
        stats.setTotalMarkets((int) allItemPrices.stream().map(ip -> ip.getMarket().getName()).distinct().count());

        if (!validPrices.isEmpty()) {
            BigDecimal sum = validPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.setOverallAveragePrice(sum.divide(BigDecimal.valueOf(validPrices.size()), 2, RoundingMode.HALF_UP));
            stats.setOverallMinPrice(validPrices.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
            stats.setOverallMaxPrice(validPrices.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));

            // 가장 저렴한/비싼 시장 찾기
            Optional<ItemPrice> cheapest = allItemPrices.stream()
                    .filter(ip -> ip.getPrice().equals(stats.getOverallMinPrice()))
                    .findFirst();
            cheapest.ifPresent(ip -> stats.setCheapestMarket(ip.getMarket().getName()));

            Optional<ItemPrice> mostExpensive = allItemPrices.stream()
                    .filter(ip -> ip.getPrice().equals(stats.getOverallMaxPrice()))
                    .findFirst();
            mostExpensive.ifPresent(ip -> stats.setMostExpensiveMarket(ip.getMarket().getName()));
        }

        return stats;
    }

    private List<MonthlyPriceInfo> generateMarketMonthlyPriceInfo(String itemName, String marketName) {
        // 구현 로직은 generateMonthlyPriceInfo와 유사하지만 특정 시장으로 필터링
        return generateMonthlyPriceInfo(itemName); // 임시로 전체 데이터 사용
    }

    private List<MarketComparisonInfo> generateMarketComparisons(String itemName, String currentMarketName, BigDecimal currentPrice) {
        List<ItemPrice> allMarketPrices = itemPriceRepository.findAllByItemName(itemName);
        List<String> largeMarts = List.of("이마트", "롯데마트", "홈플러스");

        return allMarketPrices.stream()
                .filter(ip -> !ip.getMarket().getName().equals(currentMarketName))
                .collect(Collectors.groupingBy(ip -> ip.getMarket().getName()))
                .entrySet().stream()
                .map(entry -> {
                    String marketName = entry.getKey();
                    BigDecimal marketPrice = entry.getValue().get(0).getPrice();
                    
                    MarketComparisonInfo comparison = new MarketComparisonInfo();
                    comparison.setMarketName(marketName);
                    comparison.setPrice(marketPrice);
                    comparison.setPriceDifference(marketPrice.subtract(currentPrice));
                    
                    double percentageDiff = marketPrice.subtract(currentPrice)
                            .divide(currentPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    comparison.setPercentageDifference(percentageDiff);
                    
                    if (percentageDiff > 5) {
                        comparison.setComparison("더 비쌈");
                    } else if (percentageDiff < -5) {
                        comparison.setComparison("더 저렴");
                    } else {
                        comparison.setComparison("비슷함");
                    }
                    
                    comparison.setMarketType(largeMarts.contains(marketName) ? "LARGE_MART" : "TRADITIONAL");
                    
                    return comparison;
                })
                .sorted(Comparator.comparing(MarketComparisonInfo::getPrice))
                .collect(Collectors.toList());
    }

    private PricePredictionUtil.PriceAnalysis generateMarketPriceAnalysis(String itemName, String marketName) {
        // 특정 시장의 가격 예측 (구현 간소화)
        return generatePriceAnalysis(itemName);
    }

    private String generatePurchaseRecommendation(BigDecimal currentPrice, List<MarketComparisonInfo> comparisons) {
        long cheaperMarkets = comparisons.stream()
                .filter(comp -> "더 저렴".equals(comp.getComparison()))
                .count();
        
        long expensiveMarkets = comparisons.stream()
                .filter(comp -> "더 비쌈".equals(comp.getComparison()))
                .count();

        if (cheaperMarkets == 0) {
            return "이 시장이 가장 저렴합니다! 구매 추천";
        } else if (cheaperMarkets <= 2) {
            return "상당히 경쟁력 있는 가격입니다";
        } else {
            return "다른 시장 비교 후 구매 검토 권장";
        }
    }

    private String generateComparisonSummary(String marketName, List<MarketComparisonInfo> comparisons) {
        long cheaperCount = comparisons.stream()
                .filter(comp -> "더 저렴".equals(comp.getComparison()))
                .count();
        
        long expensiveCount = comparisons.stream()
                .filter(comp -> "더 비쌈".equals(comp.getComparison()))
                .count();

        return String.format("다른 시장 중 %d곳이 더 저렴하고 %d곳이 더 비쌉니다.", cheaperCount, expensiveCount);
    }
}
