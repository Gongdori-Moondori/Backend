package khtml.backend.alzi.recommendation;

import khtml.backend.alzi.market.MarketService;
import khtml.backend.alzi.market.dto.response.MarketItemPriceResponse;
import khtml.backend.alzi.priceData.PriceDataRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final SeasonalRecommendationUtil seasonalRecommendationUtil;
    private final PricePredictionUtil pricePredictionUtil;
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final PriceDataRepository priceDataRepository;
    private final MarketService marketService;

    @Data
    public static class SmartRecommendation {
        private String itemName;
        private String category;
        private String season;
        private String seasonalReason;           // 계절적 추천 이유
        private List<String> benefits;           // 영양/건강상 이점
        private double seasonalScore;            // 계절성 점수 (0-1)
        
        // 가격 정보 (있는 경우에만)
        private BigDecimal averagePrice;         // 평균 가격
        private String priceLevel;               // LOW, MEDIUM, HIGH
        private String priceRecommendation;      // BUY, HOLD, SELL
        private double priceConfidence;          // 가격 예측 신뢰도
        private boolean hasPriceData;            // 가격 데이터 보유 여부
        
        private String overallRecommendation;    // 종합 추천 등급
        private String overallReason;            // 종합 추천 이유
    }

    @Data
    public static class MarketSavingRecommendation {
        private String itemName;
        private String category;
        private String marketName;
        
        // 가격 분석 정보
        private BigDecimal currentPrice;         // 현재 가격 (해당 시장)
        private BigDecimal averageMarketPrice;   // 전체 시장 평균 가격
        private BigDecimal savingAmount;         // 절약 금액 (평균 - 현재)
        private double savingPercentage;         // 절약 비율 (%)
        
        // 가격 예측 정보
        private String priceLevel;               // LOW, MEDIUM, HIGH
        private String recommendation;           // BUY, HOLD, SELL, STRONG_BUY
        private double confidence;               // 신뢰도 (0-1)
        private String trendAnalysis;            // 가격 추세 분석
        
        // 추가 정보
        private boolean isSeasonalItem;          // 현재 제철 아이템 여부
        private String savingReason;             // 절약 가능 이유
        private int dataPoints;                  // 분석에 사용된 데이터 포인트 수
    }

    @Data
    public static class MarketVsMartComparison {
        private String itemName;
        private String category;
        
        // 전통시장 정보
        private String marketName;
        private BigDecimal marketPrice;
        
        // 대형마트 정보  
        private String cheapestMartName;         // 가장 저렴한 대형마트명
        private BigDecimal cheapestMartPrice;    // 가장 저렴한 대형마트 가격
        private String expensiveMartName;        // 가장 비싼 대형마트명
        private BigDecimal expensiveMartPrice;   // 가장 비싼 대형마트 가격
        private BigDecimal averageMartPrice;     // 대형마트 평균 가격
        
        // 비교 분석
        private BigDecimal priceDifference;      // 가격 차이 (시장 - 마트평균)
        private double savingPercentage;         // 절약 비율 (%)
        private String winner;                   // "MARKET" or "MART" (더 저렴한 곳)
        private String recommendation;           // 구매 추천
        
        // 마트별 상세 가격 정보
        private List<MartPriceInfo> martPrices;  // 각 마트별 가격 정보
        
        // 추가 정보
        private boolean isSeasonalItem;          // 현재 제철 아이템 여부
        private String comparisonSummary;        // 비교 요약
    }

    @Data
    public static class MartPriceInfo {
        private String martName;
        private BigDecimal price;
        private String priceUnit;
        private boolean isAvailable;             // 해당 마트에서 판매 여부
    }

    @Data
    public static class ComprehensiveRecommendation {
        private String marketName;               // 분석 대상 시장명
        
        // 1. 계절 추천
        private List<SeasonalRecommendationUtil.SeasonalRecommendation> seasonalRecommendations;
        
        // 2. 시장 절약 추천  
        private List<MarketSavingRecommendation> savingRecommendations;
        
        // 3. 시장 vs 마트 비교
        private List<MarketVsMartComparison> marketVsMartComparisons;
        
        // 4. 종합 요약
        private ComprehensiveSummary summary;
    }

    @Data
    public static class ComprehensiveSummary {
        private int totalAnalyzedItems;          // 분석된 전체 아이템 수
        private int seasonalItemsCount;          // 제철 아이템 수
        private int savingItemsCount;            // 절약 가능 아이템 수
        private int marketWinCount;              // 전통시장이 더 저렴한 아이템 수
        private int martWinCount;                // 대형마트가 더 저렴한 아이템 수
        
        private BigDecimal maxSavingAmount;      // 최대 절약 가능 금액
        private double maxSavingPercentage;      // 최대 절약 비율
        private String bestDealItem;             // 가장 좋은 딜의 아이템명
        
        private String overallRecommendation;    // 종합 쇼핑 추천
        private List<String> shoppingTips;       // 쇼핑 팁
    }

    /**
     * 현재 시기에 맞는 스마트 추천 (계절성 + 가격 데이터 결합)
     */
    @Transactional(readOnly = true)
    public List<SmartRecommendation> getCurrentSmartRecommendations() {
        log.info("스마트 추천 분석 시작");

        // 1. 계절 추천 아이템 조회
        List<SeasonalRecommendationUtil.SeasonalRecommendation> seasonalRecommendations = 
            seasonalRecommendationUtil.getCurrentSeasonalRecommendations();

        // 2. 각 아이템별로 가격 데이터와 결합하여 스마트 추천 생성
        List<SmartRecommendation> smartRecommendations = seasonalRecommendations.stream()
            .map(this::createSmartRecommendation)
            .collect(Collectors.toList());

        log.info("스마트 추천 분석 완료 - {} 건", smartRecommendations.size());

        return smartRecommendations;
    }

    /**
     * 계절 추천과 가격 데이터를 결합하여 스마트 추천 생성
     */
    private SmartRecommendation createSmartRecommendation(SeasonalRecommendationUtil.SeasonalRecommendation seasonal) {
        SmartRecommendation smart = new SmartRecommendation();
        
        // 계절 정보 설정
        smart.setItemName(seasonal.getItemName());
        smart.setCategory(seasonal.getCategory());
        smart.setSeason(seasonal.getSeason());
        smart.setSeasonalReason(seasonal.getReason());
        smart.setBenefits(seasonal.getBenefits());
        smart.setSeasonalScore(seasonal.getSeasonalScore());

        // 가격 데이터 조회 시도
        try {
            analyzePriceData(smart, seasonal.getItemName());
        } catch (Exception e) {
            log.debug("아이템 '{}' 가격 데이터 조회 실패: {}", seasonal.getItemName(), e.getMessage());
            smart.setHasPriceData(false);
        }

        // 종합 추천 등급 결정
        determineOverallRecommendation(smart);

        return smart;
    }

    /**
     * 아이템의 가격 데이터 분석
     */
    private void analyzePriceData(SmartRecommendation smart, String itemName) {
        // 실제 DB에서 아이템명과 일치하는 데이터 조회
        // 여기서는 간단하게 존재 여부만 체크하고, 실제로는 가격 분석 로직을 추가할 수 있습니다.
        
        boolean hasItemInDb = itemRepository.existsByName(itemName);
        
        if (hasItemInDb) {
            smart.setHasPriceData(true);
            
            // 실제 구현에서는 여기서 PricePredictionUtil을 사용하여
            // 해당 아이템의 가격 분석을 수행할 수 있습니다.
            
            // 예시 데이터 (실제로는 분석 결과를 사용)
            smart.setAveragePrice(BigDecimal.valueOf(2500)); // 실제 계산 필요
            smart.setPriceLevel("LOW"); // 실제 분석 결과 사용
            smart.setPriceRecommendation("BUY"); // 실제 분석 결과 사용
            smart.setPriceConfidence(0.75); // 실제 신뢰도 사용
            
        } else {
            smart.setHasPriceData(false);
            log.debug("아이템 '{}'의 가격 데이터 없음", itemName);
        }
    }

    /**
     * 종합 추천 등급 결정
     */
    private void determineOverallRecommendation(SmartRecommendation smart) {
        StringBuilder reasonBuilder = new StringBuilder();
        
        // 계절성 점수가 높으면 기본적으로 좋은 추천
        if (smart.getSeasonalScore() >= 0.9) {
            smart.setOverallRecommendation("EXCELLENT");
            reasonBuilder.append("제철 최적기");
        } else if (smart.getSeasonalScore() >= 0.8) {
            smart.setOverallRecommendation("VERY_GOOD");
            reasonBuilder.append("제철 추천 시기");
        } else {
            smart.setOverallRecommendation("GOOD");
            reasonBuilder.append("계절 적합");
        }

        // 가격 데이터가 있으면 추가 고려
        if (smart.isHasPriceData()) {
            if ("BUY".equals(smart.getPriceRecommendation()) && "LOW".equals(smart.getPriceLevel())) {
                // 가격도 좋으면 추천 등급 상향
                if ("GOOD".equals(smart.getOverallRecommendation())) {
                    smart.setOverallRecommendation("VERY_GOOD");
                } else if ("VERY_GOOD".equals(smart.getOverallRecommendation())) {
                    smart.setOverallRecommendation("EXCELLENT");
                }
                reasonBuilder.append(" + 저렴한 가격");
                
            } else if ("SELL".equals(smart.getPriceRecommendation()) || "HIGH".equals(smart.getPriceLevel())) {
                // 가격이 비싸면 추천 등급 하향
                if ("EXCELLENT".equals(smart.getOverallRecommendation())) {
                    smart.setOverallRecommendation("VERY_GOOD");
                } else if ("VERY_GOOD".equals(smart.getOverallRecommendation())) {
                    smart.setOverallRecommendation("GOOD");
                }
                reasonBuilder.append(" (단, 가격 높음)");
            } else {
                reasonBuilder.append(" + 적정 가격");
            }
        } else {
            reasonBuilder.append(" (가격정보 없음)");
        }

        smart.setOverallReason(reasonBuilder.toString());
    }

    /**
     * 특정 시장에서 절약할 수 있는 아이템 추천
     */
    @Transactional(readOnly = true)
    public List<MarketSavingRecommendation> getMarketSavingRecommendations(String marketName) {
        log.info("시장 '{}' 절약 아이템 분석 시작", marketName);

        try {
            // 1. 해당 시장의 모든 아이템 가격 정보 조회
            List<MarketItemPriceResponse> marketItems = marketService.getMarketItemPrices(marketName);
            
            if (marketItems.isEmpty()) {
                log.warn("시장 '{}'에 대한 가격 데이터가 없습니다.", marketName);
                return List.of();
            }

            // 2. 각 아이템별로 절약 분석 수행
            List<MarketSavingRecommendation> savingRecommendations = marketItems.stream()
                .map(item -> analyzeMarketSaving(item, marketName))
                .filter(rec -> rec != null && rec.getSavingAmount() != null && rec.getSavingAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MarketSavingRecommendation::getSavingPercentage).reversed())
                .limit(3)
                .collect(Collectors.toList());

            log.info("시장 '{}' 절약 아이템 분석 완료 - {} 건", marketName, savingRecommendations.size());
            return savingRecommendations;

        } catch (Exception e) {
            log.error("시장 '{}' 절약 분석 중 오류 발생: {}", marketName, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 개별 아이템의 절약 가능성 분석
     */
    private MarketSavingRecommendation analyzeMarketSaving(MarketItemPriceResponse item, String marketName) {
        try {
            // 현재 시장의 최신 가격 정보 추출
            BigDecimal currentMarketPrice = getCurrentMarketPrice(item);
            if (currentMarketPrice == null || currentMarketPrice.equals(BigDecimal.ZERO)) {
                return null;
            }

            // 전체 시장 평균 가격 계산 (이 아이템에 대한)
            BigDecimal averagePrice = calculateAverageMarketPrice(item.getItemName());
            if (averagePrice == null || averagePrice.equals(BigDecimal.ZERO)) {
                return null;
            }

            // 절약 금액 및 비율 계산
            BigDecimal savingAmount = averagePrice.subtract(currentMarketPrice);
            if (savingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return null; // 절약되지 않으면 추천하지 않음
            }

            double savingPercentage = savingAmount.divide(averagePrice, 4, RoundingMode.HALF_UP)
                                                 .multiply(BigDecimal.valueOf(100))
                                                 .doubleValue();

            // 최소 5% 이상 절약되는 경우만 추천
            if (savingPercentage < 5.0) {
                return null;
            }

            MarketSavingRecommendation recommendation = new MarketSavingRecommendation();
            recommendation.setItemName(item.getItemName());
            recommendation.setCategory(item.getCategory());
            recommendation.setMarketName(marketName);
            recommendation.setCurrentPrice(currentMarketPrice);
            recommendation.setAverageMarketPrice(averagePrice);
            recommendation.setSavingAmount(savingAmount);
            recommendation.setSavingPercentage(savingPercentage);

            // 가격 예측 정보 추가
            addPricePredictionInfo(recommendation, item);

            // 계절성 정보 추가
            recommendation.setSeasonalItem(seasonalRecommendationUtil.isCurrentlyInSeason(item.getItemName()));

            // 절약 이유 설정
            setSavingReason(recommendation);

            // 데이터 포인트 수 설정
            recommendation.setDataPoints(item.getPriceDataList().size() + item.getItemPriceList().size());

            return recommendation;

        } catch (Exception e) {
            log.debug("아이템 '{}' 절약 분석 실패: {}", item.getItemName(), e.getMessage());
            return null;
        }
    }

    /**
     * 현재 시장의 최신 가격 추출
     */
    private BigDecimal getCurrentMarketPrice(MarketItemPriceResponse item) {
        // ItemPrice (현재 시세) 우선 사용
        if (!item.getItemPriceList().isEmpty()) {
            return item.getItemPriceList().get(0).getPrice();
        }
        
        // ItemPrice가 없으면 PriceData의 최신 데이터 사용
        if (!item.getPriceDataList().isEmpty()) {
            return item.getPriceDataList().get(0).getPriceAsBigDecimal();
        }
        
        return null;
    }

    /**
     * 전체 시장 평균 가격 계산 (해당 아이템)
     */
    private BigDecimal calculateAverageMarketPrice(String itemName) {
        try {
            // 모든 시장에서 해당 아이템의 가격 데이터 조회하여 평균 계산
            // 실제 구현에서는 ItemPrice와 PriceData를 모두 활용
            
            List<BigDecimal> allPrices = itemPriceRepository.findAllByItemName(itemName)
                .stream()
                .map(itemPrice -> itemPrice.getPrice())
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

            if (allPrices.isEmpty()) {
                return null;
            }

            BigDecimal sum = allPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(allPrices.size()), 2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.debug("아이템 '{}' 평균 가격 계산 실패: {}", itemName, e.getMessage());
            return null;
        }
    }

    /**
     * 가격 예측 정보 추가
     */
    private void addPricePredictionInfo(MarketSavingRecommendation recommendation, MarketItemPriceResponse item) {
        try {
            if (!item.getPriceDataList().isEmpty()) {
                PricePredictionUtil.PriceAnalysis analysis = 
                    pricePredictionUtil.analyzePriceHistory(item.getItemName(), item.getPriceDataList());
                
                if (analysis != null) {
                    recommendation.setPriceLevel(analysis.getPriceLevel());
                    recommendation.setRecommendation(analysis.getRecommendation());
                    recommendation.setConfidence(analysis.getConfidence());
                    
                    // 추세 분석
                    if (analysis.getTrendSlope() > 0.5) {
                        recommendation.setTrendAnalysis("상승 추세");
                    } else if (analysis.getTrendSlope() < -0.5) {
                        recommendation.setTrendAnalysis("하락 추세");
                    } else {
                        recommendation.setTrendAnalysis("보합 추세");
                    }
                }
            }
            
            // 기본값 설정
            if (recommendation.getPriceLevel() == null) {
                recommendation.setPriceLevel("LOW"); // 절약 가능하므로 LOW로 설정
                recommendation.setRecommendation("BUY");
                recommendation.setConfidence(0.7);
                recommendation.setTrendAnalysis("분석 데이터 부족");
            }
            
        } catch (Exception e) {
            log.debug("가격 예측 정보 추가 실패: {}", e.getMessage());
        }
    }

    /**
     * 절약 이유 설정
     */
    private void setSavingReason(MarketSavingRecommendation recommendation) {
        StringBuilder reasonBuilder = new StringBuilder();
        
        // 절약 비율에 따른 기본 메시지
        if (recommendation.getSavingPercentage() >= 30) {
            reasonBuilder.append("매우 저렴한 가격");
        } else if (recommendation.getSavingPercentage() >= 20) {
            reasonBuilder.append("상당히 저렴한 가격");  
        } else if (recommendation.getSavingPercentage() >= 10) {
            reasonBuilder.append("저렴한 가격");
        } else {
            reasonBuilder.append("약간 저렴한 가격");
        }
        
        // 제철 아이템이면 추가 정보
        if (recommendation.isSeasonalItem()) {
            reasonBuilder.append(" + 제철 아이템");
        }
        
        // 추세 분석 추가
        if ("하락 추세".equals(recommendation.getTrendAnalysis())) {
            reasonBuilder.append(" + 가격 하락 중");
        }
        
        recommendation.setSavingReason(reasonBuilder.toString());
    }

    /**
     * 전통시장 vs 대형마트 가격 비교 분석
     */
    @Transactional(readOnly = true)
    public List<MarketVsMartComparison> getMarketVsMartComparisons(String marketName) {
        log.info("전통시장 '{}' vs 대형마트 가격 비교 분석 시작", marketName);

        try {
            // 1. 대형마트 목록 정의
            List<String> largeMarts = List.of("이마트", "롯데마트", "홈플러스");
            
            // 2. 해당 시장의 모든 아이템 가격 정보 조회
            List<MarketItemPriceResponse> marketItems = marketService.getMarketItemPrices(marketName);
            
            if (marketItems.isEmpty()) {
                log.warn("시장 '{}'에 대한 가격 데이터가 없습니다.", marketName);
                return List.of();
            }

            // 3. 각 아이템별로 마트와 비교 분석
            List<MarketVsMartComparison> comparisons = marketItems.stream()
                .map(item -> compareMarketWithMarts(item, marketName, largeMarts))
                .filter(comp -> comp != null && comp.getPriceDifference() != null)
                .sorted(Comparator.comparing(MarketVsMartComparison::getSavingPercentage).reversed())
                .limit(3)
                .collect(Collectors.toList());

            log.info("전통시장 '{}' vs 대형마트 비교 분석 완료 - {} 건", marketName, comparisons.size());
            return comparisons;

        } catch (Exception e) {
            log.error("시장 '{}' vs 대형마트 비교 분석 중 오류 발생: {}", marketName, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 개별 아이템의 시장 vs 마트 비교 분석
     */
    private MarketVsMartComparison compareMarketWithMarts(MarketItemPriceResponse marketItem, 
                                                         String marketName, 
                                                         List<String> largeMarts) {
        try {
            // 시장 가격 추출
            BigDecimal marketPrice = getCurrentMarketPrice(marketItem);
            if (marketPrice == null || marketPrice.equals(BigDecimal.ZERO)) {
                return null;
            }

            // 대형마트별 가격 정보 수집
            List<MartPriceInfo> martPrices = collectMartPrices(marketItem.getItemName(), largeMarts);
            if (martPrices.isEmpty()) {
                return null; // 마트 가격 정보가 없으면 비교 불가
            }

            // 마트 가격 통계 계산
            List<BigDecimal> availableMartPrices = martPrices.stream()
                .filter(MartPriceInfo::isAvailable)
                .map(MartPriceInfo::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

            if (availableMartPrices.isEmpty()) {
                return null;
            }

            BigDecimal averageMartPrice = availableMartPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(availableMartPrices.size()), 2, RoundingMode.HALF_UP);

            BigDecimal cheapestMartPrice = availableMartPrices.stream()
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            BigDecimal expensiveMartPrice = availableMartPrices.stream()
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

            // 가장 저렴한/비싼 마트 이름 찾기
            String cheapestMartName = martPrices.stream()
                .filter(mart -> mart.getPrice() != null && mart.getPrice().equals(cheapestMartPrice))
                .findFirst()
                .map(MartPriceInfo::getMartName)
                .orElse("알수없음");

            String expensiveMartName = martPrices.stream()
                .filter(mart -> mart.getPrice() != null && mart.getPrice().equals(expensiveMartPrice))
                .findFirst()
                .map(MartPriceInfo::getMartName)
                .orElse("알수없음");

            // 가격 차이 및 절약 비율 계산
            BigDecimal priceDifference = averageMartPrice.subtract(marketPrice);
            double savingPercentage = 0.0;
            String winner = "MARKET";
            String recommendation;

            if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
                // 시장이 더 저렴한 경우
                savingPercentage = priceDifference.divide(averageMartPrice, 4, RoundingMode.HALF_UP)
                                                 .multiply(BigDecimal.valueOf(100))
                                                 .doubleValue();
                winner = "MARKET";
                
                if (savingPercentage >= 20) {
                    recommendation = "시장에서 구매 강력 추천";
                } else if (savingPercentage >= 10) {
                    recommendation = "시장에서 구매 추천";
                } else {
                    recommendation = "시장이 약간 저렴";
                }
            } else {
                // 마트가 더 저렴한 경우
                savingPercentage = priceDifference.abs().divide(marketPrice, 4, RoundingMode.HALF_UP)
                                                 .multiply(BigDecimal.valueOf(100))
                                                 .doubleValue();
                winner = "MART";
                
                if (savingPercentage >= 20) {
                    recommendation = "대형마트에서 구매 강력 추천";
                } else if (savingPercentage >= 10) {
                    recommendation = "대형마트에서 구매 추천";
                } else {
                    recommendation = "대형마트가 약간 저렴";
                }
            }

            // 비교 결과 객체 생성
            MarketVsMartComparison comparison = new MarketVsMartComparison();
            comparison.setItemName(marketItem.getItemName());
            comparison.setCategory(marketItem.getCategory());
            comparison.setMarketName(marketName);
            comparison.setMarketPrice(marketPrice);
            comparison.setCheapestMartName(cheapestMartName);
            comparison.setCheapestMartPrice(cheapestMartPrice);
            comparison.setExpensiveMartName(expensiveMartName);
            comparison.setExpensiveMartPrice(expensiveMartPrice);
            comparison.setAverageMartPrice(averageMartPrice);
            comparison.setPriceDifference(priceDifference);
            comparison.setSavingPercentage(Math.abs(savingPercentage));
            comparison.setWinner(winner);
            comparison.setRecommendation(recommendation);
            comparison.setMartPrices(martPrices);
            comparison.setSeasonalItem(seasonalRecommendationUtil.isCurrentlyInSeason(marketItem.getItemName()));
            comparison.setComparisonSummary(generateComparisonSummary(comparison));

            return comparison;

        } catch (Exception e) {
            log.debug("아이템 '{}' 시장 vs 마트 비교 실패: {}", marketItem.getItemName(), e.getMessage());
            return null;
        }
    }

    /**
     * 대형마트별 가격 정보 수집
     */
    private List<MartPriceInfo> collectMartPrices(String itemName, List<String> largeMarts) {
        return largeMarts.stream()
            .map(martName -> {
                MartPriceInfo martInfo = new MartPriceInfo();
                martInfo.setMartName(martName);
                
                try {
                    // 해당 마트에서 해당 아이템의 가격 정보 조회
                    List<ItemPrice> martPrices = itemPriceRepository.findByMarketNameAndItemName(martName, itemName);
                    
                    if (!martPrices.isEmpty()) {
                        // 가장 최신 가격 정보 사용
                        ItemPrice latestPrice = martPrices.get(0);
                        martInfo.setPrice(latestPrice.getPrice());
                        martInfo.setPriceUnit(latestPrice.getPriceUnit());
                        martInfo.setAvailable(true);
                    } else {
                        martInfo.setAvailable(false);
                        log.debug("마트 '{}'에서 아이템 '{}' 가격 정보 없음", martName, itemName);
                    }
                } catch (Exception e) {
                    martInfo.setAvailable(false);
                    log.debug("마트 '{}' 아이템 '{}' 가격 조회 실패: {}", martName, itemName, e.getMessage());
                }
                
                return martInfo;
            })
            .collect(Collectors.toList());
    }

    /**
     * 비교 요약 생성
     */
    private String generateComparisonSummary(MarketVsMartComparison comparison) {
        StringBuilder summary = new StringBuilder();
        
        if ("MARKET".equals(comparison.getWinner())) {
            summary.append(String.format("%s가 대형마트 평균보다 %.1f%% 저렴", 
                         comparison.getMarketName(), comparison.getSavingPercentage()));
            
            if (comparison.getSavingPercentage() >= 20) {
                summary.append(" (큰 차이)");
            } else if (comparison.getSavingPercentage() >= 10) {
                summary.append(" (어느 정도 차이)");
            }
        } else {
            summary.append(String.format("대형마트가 %s보다 %.1f%% 저렴", 
                         comparison.getMarketName(), comparison.getSavingPercentage()));
        }
        
        if (comparison.isSeasonalItem()) {
            summary.append(" + 제철 아이템");
        }
        
        return summary.toString();
    }

    /**
     * 종합 추천 분석 (계절 + 절약 + 시장vs마트 통합)
     */
    @Transactional(readOnly = true)
    public ComprehensiveRecommendation getComprehensiveRecommendation(String marketName) {
        log.info("시장 '{}' 종합 추천 분석 시작", marketName);

        try {
            ComprehensiveRecommendation comprehensive = new ComprehensiveRecommendation();
            comprehensive.setMarketName(marketName);

            // 1. 계절 추천 조회
            List<SeasonalRecommendationUtil.SeasonalRecommendation> seasonalRecommendations = 
                seasonalRecommendationUtil.getCurrentSeasonalRecommendations();
            comprehensive.setSeasonalRecommendations(seasonalRecommendations);

            // 2. 시장 절약 추천 조회
            List<MarketSavingRecommendation> savingRecommendations = 
                getMarketSavingRecommendations(marketName);
            comprehensive.setSavingRecommendations(savingRecommendations);

            // 3. 시장 vs 마트 비교 조회
            List<MarketVsMartComparison> marketVsMartComparisons = 
                getMarketVsMartComparisons(marketName);
            comprehensive.setMarketVsMartComparisons(marketVsMartComparisons);

            // 4. 종합 요약 생성
            ComprehensiveSummary summary = generateComprehensiveSummary(
                seasonalRecommendations, savingRecommendations, marketVsMartComparisons, marketName);
            comprehensive.setSummary(summary);

            log.info("시장 '{}' 종합 추천 분석 완료", marketName);
            return comprehensive;

        } catch (Exception e) {
            log.error("시장 '{}' 종합 추천 분석 중 오류 발생: {}", marketName, e.getMessage(), e);
            throw new RuntimeException("종합 추천 분석 실패", e);
        }
    }

    /**
     * 종합 요약 생성
     */
    private ComprehensiveSummary generateComprehensiveSummary(
            List<SeasonalRecommendationUtil.SeasonalRecommendation> seasonalRecs,
            List<MarketSavingRecommendation> savingRecs,
            List<MarketVsMartComparison> martComparisons,
            String marketName) {

        ComprehensiveSummary summary = new ComprehensiveSummary();

        // 기본 통계
        summary.setSeasonalItemsCount(seasonalRecs.size());
        summary.setSavingItemsCount(savingRecs.size());
        
        // 시장 vs 마트 승패 계산
        int marketWins = (int) martComparisons.stream()
            .filter(comp -> "MARKET".equals(comp.getWinner()))
            .count();
        int martWins = (int) martComparisons.stream()
            .filter(comp -> "MART".equals(comp.getWinner()))
            .count();
        
        summary.setMarketWinCount(marketWins);
        summary.setMartWinCount(martWins);
        summary.setTotalAnalyzedItems(martComparisons.size());

        // 최대 절약 정보 계산
        calculateMaxSavingInfo(summary, savingRecs, martComparisons);

        // 종합 쇼핑 추천 생성
        generateOverallRecommendation(summary, marketName, marketWins, martWins);

        // 쇼핑 팁 생성
        generateShoppingTips(summary, seasonalRecs, savingRecs, martComparisons);

        return summary;
    }

    /**
     * 최대 절약 정보 계산
     */
    private void calculateMaxSavingInfo(ComprehensiveSummary summary, 
                                       List<MarketSavingRecommendation> savingRecs,
                                       List<MarketVsMartComparison> martComparisons) {
        
        BigDecimal maxSaving = BigDecimal.ZERO;
        double maxSavingPercent = 0.0;
        String bestDealItem = "없음";

        // 절약 추천에서 최대값 찾기
        for (MarketSavingRecommendation rec : savingRecs) {
            if (rec.getSavingAmount() != null && rec.getSavingAmount().compareTo(maxSaving) > 0) {
                maxSaving = rec.getSavingAmount();
                maxSavingPercent = rec.getSavingPercentage();
                bestDealItem = rec.getItemName();
            }
        }

        // 마트 비교에서도 최대값 확인 (시장이 더 저렴한 경우)
        for (MarketVsMartComparison comp : martComparisons) {
            if ("MARKET".equals(comp.getWinner()) && 
                comp.getPriceDifference() != null && 
                comp.getPriceDifference().compareTo(maxSaving) > 0) {
                
                maxSaving = comp.getPriceDifference();
                maxSavingPercent = comp.getSavingPercentage();
                bestDealItem = comp.getItemName();
            }
        }

        summary.setMaxSavingAmount(maxSaving);
        summary.setMaxSavingPercentage(maxSavingPercent);
        summary.setBestDealItem(bestDealItem);
    }

    /**
     * 종합 쇼핑 추천 생성
     */
    private void generateOverallRecommendation(ComprehensiveSummary summary, String marketName, 
                                             int marketWins, int martWins) {
        StringBuilder recommendation = new StringBuilder();

        if (marketWins > martWins) {
            recommendation.append(String.format("%s에서 쇼핑하는 것을 추천합니다! ", marketName));
            recommendation.append(String.format("분석한 %d개 아이템 중 %d개에서 더 저렴합니다.", 
                                               marketWins + martWins, marketWins));
        } else if (martWins > marketWins) {
            recommendation.append("대형마트에서 쇼핑하는 것이 더 경제적입니다. ");
            recommendation.append(String.format("분석한 %d개 아이템 중 %d개에서 마트가 더 저렴합니다.", 
                                               marketWins + martWins, martWins));
        } else {
            recommendation.append("전통시장과 대형마트가 비슷한 가격 수준입니다. ");
            recommendation.append("아이템별로 선택적 구매를 추천합니다.");
        }

        // 제철 아이템 추가 정보
        if (summary.getSeasonalItemsCount() > 0) {
            recommendation.append(String.format(" 현재 %d개의 제철 아이템이 있으니 함께 고려해보세요.", 
                                               summary.getSeasonalItemsCount()));
        }

        summary.setOverallRecommendation(recommendation.toString());
    }

    /**
     * 쇼핑 팁 생성
     */
    private void generateShoppingTips(ComprehensiveSummary summary,
                                     List<SeasonalRecommendationUtil.SeasonalRecommendation> seasonalRecs,
                                     List<MarketSavingRecommendation> savingRecs,
                                     List<MarketVsMartComparison> martComparisons) {
        
        List<String> tips = new ArrayList<>();

        // 제철 아이템 팁
        if (!seasonalRecs.isEmpty()) {
            String seasonalItems = seasonalRecs.stream()
                .map(SeasonalRecommendationUtil.SeasonalRecommendation::getItemName)
                .collect(Collectors.joining(", "));
            tips.add("🍎 제철 아이템: " + seasonalItems + " - 영양가도 높고 맛도 좋습니다!");
        }

        // 절약 팁
        if (!savingRecs.isEmpty()) {
            MarketSavingRecommendation topSaving = savingRecs.get(0);
            tips.add(String.format("💰 최고 절약템: %s (%.1f%% 절약)", 
                                 topSaving.getItemName(), topSaving.getSavingPercentage()));
        }

        // 시장 vs 마트 팁
        long marketAdvantageItems = martComparisons.stream()
            .filter(comp -> "MARKET".equals(comp.getWinner()))
            .count();
        
        if (marketAdvantageItems > 0) {
            tips.add(String.format("🏪 전통시장 유리 아이템 %d개 - 신선하고 저렴합니다!", marketAdvantageItems));
        }

        long martAdvantageItems = martComparisons.stream()
            .filter(comp -> "MART".equals(comp.getWinner()))
            .count();
        
        if (martAdvantageItems > 0) {
            tips.add(String.format("🏬 대형마트 유리 아이템 %d개 - 편리하고 품질이 일정합니다!", martAdvantageItems));
        }

        // 최대 절약 팁
        if (summary.getMaxSavingAmount() != null && 
            summary.getMaxSavingAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            tips.add(String.format("💸 '%s' 구매시 최대 %,d원 절약 가능!", 
                                 summary.getBestDealItem(), summary.getMaxSavingAmount().intValue()));
        }

        // 기본 팁 추가
        if (tips.isEmpty()) {
            tips.add("📊 다양한 옵션을 비교해보고 현명한 쇼핑하세요!");
        }

        summary.setShoppingTips(tips);
    }
}
