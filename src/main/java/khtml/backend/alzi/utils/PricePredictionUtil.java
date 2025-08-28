package khtml.backend.alzi.utils;

import khtml.backend.alzi.market.dto.response.MarketItemPriceResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 없이 수학적/통계적 방법으로 가격 예측 및 분석
 * 업그레이드 버전 - RSI, 볼린저 밴드, 계절성 분석 포함
 */
@Component
public class PricePredictionUtil {
    
    @Data
    public static class PriceAnalysis {
        private String name;
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
        private String recommendation; // BUY, HOLD, SELL, STRONG_BUY, STRONG_SELL
        private double confidence; // 신뢰도 (0-1)
        
        // 새로운 기술적 지표들
        private double rsi14;                    // 14일 RSI
        private BollingerBands bollingerBands;   // 볼린저 밴드
        private double seasonalityScore;         // 계절성 점수 (0-1)
        private String marketSentiment;          // "BULLISH", "BEARISH", "NEUTRAL"
        private BigDecimal predictedPrice7Days;  // 7일 후 예상가격
        private BigDecimal predictedPrice30Days; // 30일 후 예상가격
        private List<String> riskFactors;        // 리스크 요인들
    }
    
    @Data
    public static class BollingerBands {
        private BigDecimal upperBand;
        private BigDecimal middleBand; // 이동평균
        private BigDecimal lowerBand;
        private String position; // "ABOVE_UPPER", "BETWEEN", "BELOW_LOWER"
    }
    
    /**
     * 특정 아이템의 가격 분석 및 예측 (업그레이드 버전)
     */
    public PriceAnalysis analyzePriceHistory(String name, List<MarketItemPriceResponse.PriceDataInfo> priceHistory) {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        
        // 날짜순 정렬 (최신순)
        List<MarketItemPriceResponse.PriceDataInfo> sortedPrices = priceHistory.stream()
                .sorted(Comparator.comparing(MarketItemPriceResponse.PriceDataInfo::getDate).reversed())
                .toList();
        
        PriceAnalysis analysis = new PriceAnalysis();
        analysis.setName(name);
        
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
        
        // 새로운 기술적 지표들
        analysis.setRsi14(calculateRSI(sortedPrices, 14));
        analysis.setBollingerBands(calculateBollingerBands(sortedPrices, 20));
        analysis.setSeasonalityScore(calculateSeasonality(sortedPrices));
        analysis.setMarketSentiment(determineMarketSentiment(analysis));
        
        // 가격 예측
        analysis.setPredictedPrice7Days(predictPrice(sortedPrices, 7, analysis));
        analysis.setPredictedPrice30Days(predictPrice(sortedPrices, 30, analysis));
        
        // 리스크 요인 분석
        analysis.setRiskFactors(identifyRiskFactors(analysis));
        
        // 개선된 추천 및 신뢰도
        String[] recommendation = generateEnhancedRecommendation(analysis);
        analysis.setRecommendation(recommendation[0]);
        analysis.setConfidence(Double.parseDouble(recommendation[1]));
        
        return analysis;
    }
    
    /**
     * RSI (상대강도지수) 계산
     */
    private double calculateRSI(List<MarketItemPriceResponse.PriceDataInfo> prices, int period) {
        if (prices.size() < period + 1) return 50.0; // 중립
        
        double avgGain = 0, avgLoss = 0;
        
        for (int i = 1; i <= period; i++) {
            double change = prices.get(i-1).getPriceAsBigDecimal().doubleValue() 
                           - prices.get(i).getPriceAsBigDecimal().doubleValue();
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }
        
        avgGain /= period;
        avgLoss /= period;
        
        if (avgLoss == 0) return 100.0;
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
    
    /**
     * 볼린저 밴드 계산
     */
    private BollingerBands calculateBollingerBands(List<MarketItemPriceResponse.PriceDataInfo> prices, int period) {
        BigDecimal ma = calculateMovingAverage(prices, period);
        double stdDev = calculateStandardDeviation(prices, period);
        
        BigDecimal upperBand = ma.add(BigDecimal.valueOf(2 * stdDev));
        BigDecimal lowerBand = ma.subtract(BigDecimal.valueOf(2 * stdDev));
        
        BigDecimal currentPrice = prices.get(0).getPriceAsBigDecimal();
        String position;
        
        if (currentPrice.compareTo(upperBand) > 0) {
            position = "ABOVE_UPPER"; // 과매수
        } else if (currentPrice.compareTo(lowerBand) < 0) {
            position = "BELOW_LOWER"; // 과매도
        } else {
            position = "BETWEEN"; // 정상 범위
        }
        
        BollingerBands bands = new BollingerBands();
        bands.setUpperBand(upperBand);
        bands.setMiddleBand(ma);
        bands.setLowerBand(lowerBand);
        bands.setPosition(position);
        
        return bands;
    }
    
    /**
     * 계절성 분석
     */
    private double calculateSeasonality(List<MarketItemPriceResponse.PriceDataInfo> prices) {
        if (prices.size() < 12) return 0.5; // 데이터 부족시 중립
        
        // 월별 평균 가격 계산
        Map<Integer, List<BigDecimal>> monthlyPrices = new HashMap<>();
        
        for (var price : prices) {
            int month = price.getDate().getMonthValue();
            monthlyPrices.computeIfAbsent(month, k -> new ArrayList<>())
                        .add(price.getPriceAsBigDecimal());
        }
        
        // 월별 평균값 계산
        Map<Integer, BigDecimal> monthlyAverages = monthlyPrices.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<BigDecimal> monthPrices = entry.getValue();
                            BigDecimal sum = monthPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                            return sum.divide(BigDecimal.valueOf(monthPrices.size()), 2, RoundingMode.HALF_UP);
                        }
                ));
        
        // 현재 월의 계절성 점수 계산
        int currentMonth = prices.get(0).getDate().getMonthValue();
        if (!monthlyAverages.containsKey(currentMonth)) {
            return 0.5;
        }
        
        BigDecimal currentMonthAvg = monthlyAverages.get(currentMonth);
        BigDecimal overallAvg = monthlyAverages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlyAverages.size()), 2, RoundingMode.HALF_UP);
        
        // 현재 월이 평균보다 얼마나 유리한지 계산 (0-1 스케일)
        double ratio = currentMonthAvg.divide(overallAvg, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.min(1.0, Math.max(0.0, (ratio - 0.5) * 2)); // 0.5를 기준으로 0-1 스케일로 변환
    }
    
    /**
     * 표준편차 계산
     */
    private double calculateStandardDeviation(List<MarketItemPriceResponse.PriceDataInfo> prices, int period) {
        if (prices.size() < period) {
            period = prices.size();
        }
        
        BigDecimal average = calculateMovingAverage(prices, period);
        
        double variance = prices.stream()
                .limit(period)
                .mapToDouble(p -> {
                    double diff = p.getPriceAsBigDecimal().subtract(average).doubleValue();
                    return diff * diff;
                })
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * 시장 심리 결정
     */
    private String determineMarketSentiment(PriceAnalysis analysis) {
        int bullishSignals = 0;
        int bearishSignals = 0;
        
        // 추세 분석
        if (analysis.getTrendSlope() > 0.5) bullishSignals++;
        else if (analysis.getTrendSlope() < -0.5) bearishSignals++;
        
        // RSI 분석
        if (analysis.getRsi14() > 70) bearishSignals++; // 과매수
        else if (analysis.getRsi14() < 30) bullishSignals++; // 과매도
        
        // 볼린저 밴드 분석
        switch (analysis.getBollingerBands().getPosition()) {
            case "BELOW_LOWER" -> bullishSignals++;
            case "ABOVE_UPPER" -> bearishSignals++;
        }
        
        // 이동평균 분석
        if (analysis.getCurrentPrice().compareTo(analysis.getMovingAverage30Days()) > 0) {
            bullishSignals++;
        } else {
            bearishSignals++;
        }
        
        if (bullishSignals > bearishSignals) {
            return "BULLISH";
        } else if (bearishSignals > bullishSignals) {
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }
    
    /**
     * 가격 예측
     */
    private BigDecimal predictPrice(List<MarketItemPriceResponse.PriceDataInfo> prices, int daysAhead, PriceAnalysis analysis) {
        BigDecimal currentPrice = analysis.getCurrentPrice();
        
        // 다중 요인 예측 모델
        double trendComponent = analysis.getTrendSlope() * daysAhead;
        double seasonalComponent = (analysis.getSeasonalityScore() - 0.5) * 0.1 * daysAhead; // 계절성 영향
        double volatilityComponent = analysis.getVolatility() * 0.05; // 변동성 고려
        
        // RSI 기반 조정
        double rsiAdjustment = 0;
        if (analysis.getRsi14() > 70) {
            rsiAdjustment = -volatilityComponent; // 과매수시 하향 조정
        } else if (analysis.getRsi14() < 30) {
            rsiAdjustment = volatilityComponent; // 과매도시 상향 조정
        }
        
        double totalChange = trendComponent + seasonalComponent + rsiAdjustment;
        
        BigDecimal predictedPrice = currentPrice.add(BigDecimal.valueOf(totalChange));
        
        // 음수 방지 및 합리적 범위 제한
        BigDecimal minReasonable = analysis.getMinPrice().multiply(BigDecimal.valueOf(0.5));
        BigDecimal maxReasonable = analysis.getMaxPrice().multiply(BigDecimal.valueOf(2.0));
        
        return predictedPrice.max(minReasonable).min(maxReasonable);
    }
    
    /**
     * 리스크 요인 식별
     */
    private List<String> identifyRiskFactors(PriceAnalysis analysis) {
        List<String> riskFactors = new ArrayList<>();
        
        // 높은 변동성
        if (analysis.getVolatility() > 20) {
            riskFactors.add("높은 가격 변동성");
        }
        
        // 극단적인 RSI
        if (analysis.getRsi14() > 80) {
            riskFactors.add("극도의 과매수 상태");
        } else if (analysis.getRsi14() < 20) {
            riskFactors.add("극도의 과매도 상태");
        }
        
        // 볼린저 밴드 이탈
        if ("ABOVE_UPPER".equals(analysis.getBollingerBands().getPosition())) {
            riskFactors.add("볼린저 밴드 상단 이탈 (과매수)");
        } else if ("BELOW_LOWER".equals(analysis.getBollingerBands().getPosition())) {
            riskFactors.add("볼린저 밴드 하단 이탈 (과매도)");
        }
        
        // 가격 수준 리스크
        if ("HIGH".equals(analysis.getPriceLevel())) {
            riskFactors.add("역사적 고가 구간");
        }
        
        // 급격한 추세 변화
        if (Math.abs(analysis.getTrendSlope()) > 2.0) {
            riskFactors.add("급격한 가격 추세 변화");
        }
        
        if (riskFactors.isEmpty()) {
            riskFactors.add("주요 리스크 없음");
        }
        
        return riskFactors;
    }
    
    /**
     * 개선된 구매 추천 및 신뢰도 계산
     */
    private String[] generateEnhancedRecommendation(PriceAnalysis analysis) {
        double score = 0.0;
        double maxScore = 0.0;
        
        // 1. 가격 수준 점수 (25점)
        switch (analysis.getPriceLevel()) {
            case "LOW" -> score += 25;
            case "MEDIUM" -> score += 12;
            case "HIGH" -> score += 0;
        }
        maxScore += 25;
        
        // 2. 추세 점수 (20점)
        if (analysis.getTrendSlope() < -0.5) { // 하락 추세
            score += 20;
        } else if (analysis.getTrendSlope() < 0.5) { // 보합
            score += 8;
        } else { // 상승 추세
            score += 0;
        }
        maxScore += 20;
        
        // 3. 이동평균 대비 점수 (20점)
        BigDecimal current = analysis.getCurrentPrice();
        BigDecimal ma30 = analysis.getMovingAverage30Days();
        if (ma30 != null && current != null && current.compareTo(ma30) < 0) {
            score += 20;
        } else if (ma30 != null) {
            score += 4;
        }
        maxScore += 20;
        
        // 4. RSI 점수 (15점)
        if (analysis.getRsi14() < 30) { // 과매도
            score += 15;
        } else if (analysis.getRsi14() > 70) { // 과매수
            score += 0;
        } else {
            score += 5;
        }
        maxScore += 15;
        
        // 5. 볼린저 밴드 점수 (10점)
        switch (analysis.getBollingerBands().getPosition()) {
            case "BELOW_LOWER" -> score += 10; // 매수 신호
            case "ABOVE_UPPER" -> score += 0;  // 매도 신호
            default -> score += 5;
        }
        maxScore += 10;
        
        // 6. 계절성 점수 (10점)
        if (analysis.getSeasonalityScore() > 0.7) {
            score += 10; // 계절적으로 유리한 시기
        } else if (analysis.getSeasonalityScore() > 0.3) {
            score += 5;
        }
        maxScore += 10;
        
        double confidence = maxScore > 0 ? score / maxScore : 0;
        
        String recommendation;
        if (confidence >= 0.8) {
            recommendation = "STRONG_BUY";
        } else if (confidence >= 0.65) {
            recommendation = "BUY";
        } else if (confidence >= 0.35) {
            recommendation = "HOLD";
        } else if (confidence >= 0.2) {
            recommendation = "SELL";
        } else {
            recommendation = "STRONG_SELL";
        }
        
        return new String[]{recommendation, String.valueOf(confidence)};
    }
    
    // === 기존 메서드들 유지 ===
    
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
}
