package khtml.backend.alzi.utils;

import khtml.backend.alzi.market.dto.response.MarketItemPriceResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * AI 없이 수학적/통계적 방법으로 가격 예측 및 분석
 */
@Component
public class PricePredictionUtil {
    
    @Data
    public static class PriceAnalysis {
        private BigDecimal currentPrice;
        private BigDecimal averagePrice;
        private BigDecimal medianPrice;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal movingAverage7Days;
        private BigDecimal movingAverage30Days;
        private double volatility; // 변동성 (표준편차)
        private double trendSlope; // 추세 기울기
        private String priceLevel; // LOW, MEDIUM, HIGH
        private String recommendation; // BUY, HOLD, SELL
        private double confidence; // 신뢰도 (0-1)
    }
    
    /**
     * 특정 아이템의 가격 분석 및 예측
     */
    public PriceAnalysis analyzePriceHistory(List<MarketItemPriceResponse.PriceDataInfo> priceHistory) {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        
        // 날짜순 정렬 (최신순)
        List<MarketItemPriceResponse.PriceDataInfo> sortedPrices = priceHistory.stream()
                .sorted(Comparator.comparing(MarketItemPriceResponse.PriceDataInfo::getDate).reversed())
                .toList();
        
        PriceAnalysis analysis = new PriceAnalysis();
        
        // 기본 통계
        analysis.setCurrentPrice(sortedPrices.get(0).getPriceAsBigDecimal());
        analysis.setAveragePrice(calculateAverage(sortedPrices));
        analysis.setMedianPrice(calculateMedian(sortedPrices));
        analysis.setMinPrice(calculateMin(sortedPrices));
        analysis.setMaxPrice(calculateMax(sortedPrices));
        
        // 이동평균
        analysis.setMovingAverage7Days(calculateMovingAverage(sortedPrices, 7));
        analysis.setMovingAverage30Days(calculateMovingAverage(sortedPrices, 30));
        
        // 변동성 계산
        analysis.setVolatility(calculateVolatility(sortedPrices));
        
        // 추세 계산 (선형 회귀 기울기)
        analysis.setTrendSlope(calculateTrend(sortedPrices));
        
        // 현재 가격 수준 평가
        analysis.setPriceLevel(evaluatePriceLevel(analysis));
        
        // 추천 및 신뢰도
        String[] recommendation = generateRecommendation(analysis);
        analysis.setRecommendation(recommendation[0]);
        analysis.setConfidence(Double.parseDouble(recommendation[1]));
        
        return analysis;
    }
    
    /**
     * 평균 가격 계산
     */
    private BigDecimal calculateAverage(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        BigDecimal sum = prices.stream()
                .map(MarketItemPriceResponse.PriceDataInfo::getPriceAsBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 중앙값 계산
     */
    private BigDecimal calculateMedian(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        List<BigDecimal> sortedPrices = prices.stream()
                .map(MarketItemPriceResponse.PriceDataInfo::getPriceAsBigDecimal)
                .sorted()
                .toList();
        
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return sortedPrices.get(size / 2 - 1)
                    .add(sortedPrices.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            return sortedPrices.get(size / 2);
        }
    }
    
    private BigDecimal calculateMin(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        return prices.stream()
                .map(MarketItemPriceResponse.PriceDataInfo::getPriceAsBigDecimal)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal calculateMax(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        return prices.stream()
                .map(MarketItemPriceResponse.PriceDataInfo::getPriceAsBigDecimal)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * 이동평균 계산
     */
    private BigDecimal calculateMovingAverage(List<MarketItemPriceResponse.PriceDataInfo> prices, int period) {
        if (prices.size() < period) {
            return calculateAverage(prices);
        }
        
        BigDecimal sum = prices.stream()
                .limit(period)
                .map(MarketItemPriceResponse.PriceDataInfo::getPriceAsBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 변동성 계산 (표준편차)
     */
    private double calculateVolatility(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        BigDecimal average = calculateAverage(prices);
        
        double variance = prices.stream()
                .mapToDouble(p -> {
                    double diff = p.getPriceAsBigDecimal().subtract(average).doubleValue();
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 추세 계산 (선형 회귀의 기울기)
     */
    private double calculateTrend(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        if (prices.size() < 2) return 0.0;
        
        // 최근 30일 또는 전체 데이터 중 작은 것 사용
        int n = Math.min(prices.size(), 30);
        List<MarketItemPriceResponse.PriceDataInfo> recentPrices = prices.subList(0, n);
        
        // x = 날짜 인덱스, y = 가격
        double sumX = IntStream.range(0, n).sum();
        double sumY = recentPrices.stream()
                .mapToDouble(p -> p.getPriceAsBigDecimal().doubleValue())
                .sum();
        double sumXY = IntStream.range(0, n)
                .mapToDouble(i -> i * recentPrices.get(i).getPriceAsBigDecimal().doubleValue())
                .sum();
        double sumXX = IntStream.range(0, n)
                .mapToDouble(i -> i * i)
                .sum();
        
        // 선형회귀 기울기: (n*sumXY - sumX*sumY) / (n*sumXX - sumX*sumX)
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    /**
     * 현재 가격 수준 평가
     */
    private String evaluatePriceLevel(PriceAnalysis analysis) {
        BigDecimal current = analysis.getCurrentPrice();
        BigDecimal min = analysis.getMinPrice();
        BigDecimal max = analysis.getMaxPrice();
        
        if (current == null || min == null || max == null) {
            return "UNKNOWN";
        }
        
        // 가격 범위를 3등분하여 LOW, MEDIUM, HIGH 구분
        BigDecimal range = max.subtract(min);
        if (range.equals(BigDecimal.ZERO)) {
            return "MEDIUM";
        }
        
        BigDecimal lowThreshold = min.add(range.multiply(BigDecimal.valueOf(0.33)));
        BigDecimal highThreshold = min.add(range.multiply(BigDecimal.valueOf(0.67)));
        
        if (current.compareTo(lowThreshold) < 0) {
            return "LOW";
        } else if (current.compareTo(highThreshold) > 0) {
            return "HIGH";
        } else {
            return "MEDIUM";
        }
    }
    
    /**
     * 구매 추천 및 신뢰도 계산
     */
    private String[] generateRecommendation(PriceAnalysis analysis) {
        double score = 0.0;
        double maxScore = 0.0;
        
        // 1. 가격 수준 점수 (30점)
        switch (analysis.getPriceLevel()) {
            case "LOW" -> score += 30;
            case "MEDIUM" -> score += 15;
            case "HIGH" -> score += 0;
        }
        maxScore += 30;
        
        // 2. 추세 점수 (25점)
        if (analysis.getTrendSlope() < -0.5) { // 하락 추세
            score += 25;
        } else if (analysis.getTrendSlope() < 0.5) { // 보합
            score += 10;
        } else { // 상승 추세
            score += 0;
        }
        maxScore += 25;
        
        // 3. 이동평균 대비 점수 (25점)
        BigDecimal current = analysis.getCurrentPrice();
        BigDecimal ma30 = analysis.getMovingAverage30Days();
        if (ma30 != null && current != null && current.compareTo(ma30) < 0) { // 현재 가격이 이동평균 아래
            score += 25;
        } else if (ma30 != null) {
            score += 5;
        }
        maxScore += 25;
        
        // 4. 변동성 점수 (20점) - 변동성이 낮을수록 안정적
        if (analysis.getVolatility() < 10) {
            score += 20;
        } else if (analysis.getVolatility() < 20) {
            score += 10;
        }
        maxScore += 20;
        
        double confidence = maxScore > 0 ? score / maxScore : 0;
        String recommendation;
        
        if (confidence >= 0.7) {
            recommendation = "BUY"; // 구매 추천
        } else if (confidence >= 0.4) {
            recommendation = "HOLD"; // 보유
        } else {
            recommendation = "SELL"; // 판매 또는 구매 연기
        }
        
        return new String[]{recommendation, String.valueOf(confidence)};
    }
}
