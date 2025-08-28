package khtml.backend.alzi.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.utils.ApiResponse;
import khtml.backend.alzi.utils.SeasonalRecommendationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/recommendation")
@Tag(name = "Recommendation API", description = "계절별 아이템 추천 API")
@Slf4j
public class RecommendationController {

    private final SeasonalRecommendationUtil seasonalRecommendationUtil;
    private final RecommendationService recommendationService;

    @GetMapping("/seasonal")
    @Operation(
        summary = "현재 시기 추천 아이템 조회", 
        description = "현재 월/계절에 맞는 저렴하고 맛있는 제철 아이템 3개를 추천합니다. " +
                     "각 아이템별로 추천 이유와 영양 정보도 함께 제공됩니다."
    )
    public ApiResponse<List<SeasonalRecommendationUtil.SeasonalRecommendation>> getCurrentSeasonalRecommendations() {
        
        log.info("현재 시기 계절 추천 아이템 조회 요청 - 날짜: {}", LocalDate.now());

        try {
            List<SeasonalRecommendationUtil.SeasonalRecommendation> recommendations = 
                seasonalRecommendationUtil.getCurrentSeasonalRecommendations();

            String currentMonth = LocalDate.now().getMonth().name();
            String message = String.format("현재 시기(%s)에 추천하는 제철 아이템입니다.", currentMonth);

            log.info("계절 추천 아이템 조회 완료 - {} 건", recommendations.size());

            return ApiResponse.success(message, recommendations);

        } catch (Exception e) {
            log.error("계절 추천 아이템 조회 중 오류 발생", e);
            return ApiResponse.failure("SEASONAL_RECOMMENDATION_FAILED", 
                "계절 추천 아이템 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/smart")
    @Operation(
        summary = "스마트 추천 아이템 조회", 
        description = "현재 시기에 맞는 제철 아이템 + 가격 분석을 결합한 스마트 추천을 제공합니다. " +
                     "계절성과 가격 데이터를 모두 고려하여 최적의 구매 시점을 추천합니다."
    )
    public ApiResponse<List<RecommendationService.SmartRecommendation>> getCurrentSmartRecommendations() {
        
        log.info("현재 시기 스마트 추천 아이템 조회 요청 - 날짜: {}", LocalDate.now());

        try {
            List<RecommendationService.SmartRecommendation> recommendations = 
                recommendationService.getCurrentSmartRecommendations();

            String message = String.format("현재 시기에 맞는 스마트 추천입니다. (제철 + 가격 분석 결합)");

            log.info("스마트 추천 아이템 조회 완료 - {} 건", recommendations.size());

            return ApiResponse.success(message, recommendations);

        } catch (Exception e) {
            log.error("스마트 추천 아이템 조회 중 오류 발생", e);
            return ApiResponse.failure("SMART_RECOMMENDATION_FAILED", 
                "스마트 추천 아이템 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/market/{marketName}/saving")
    @Operation(
        summary = "시장별 절약 아이템 추천", 
        description = "특정 시장에서 현재 구매 시 가장 절약할 수 있는 아이템 3개를 추천합니다. " +
                     "전체 시장 평균 대비 저렴한 가격과 절약 금액/비율을 함께 제공합니다."
    )
    public ApiResponse<List<RecommendationService.MarketSavingRecommendation>> getMarketSavingRecommendations(
            @Parameter(description = "시장명") @PathVariable String marketName) {
        
        log.info("시장 '{}' 절약 아이템 추천 요청", marketName);

        try {
            List<RecommendationService.MarketSavingRecommendation> recommendations = 
                recommendationService.getMarketSavingRecommendations(marketName);

            if (recommendations.isEmpty()) {
                return ApiResponse.success(
                    String.format("시장 '%s'에 대한 절약 가능한 아이템이 없거나 데이터가 부족합니다.", marketName), 
                    recommendations);
            }

            String message = String.format("시장 '%s'에서 현재 가장 절약할 수 있는 아이템 %d개입니다.", 
                                         marketName, recommendations.size());

            log.info("시장 '{}' 절약 아이템 추천 완료 - {} 건", marketName, recommendations.size());

            return ApiResponse.success(message, recommendations);

        } catch (Exception e) {
            log.error("시장 '{}' 절약 아이템 추천 중 오류 발생", marketName, e);
            return ApiResponse.failure("MARKET_SAVING_RECOMMENDATION_FAILED", 
                "시장 절약 아이템 추천 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/market/{marketName}/vs-mart")
    @Operation(
        summary = "전통시장 vs 대형마트 가격 비교", 
        description = "특정 전통시장과 대형마트(이마트, 롯데마트, 홈플러스) 간 가격 차이가 가장 큰 아이템 3개를 분석합니다. " +
                     "각 아이템별로 시장과 마트 중 어디서 사는 것이 더 유리한지 추천해드립니다."
    )
    public ApiResponse<List<RecommendationService.MarketVsMartComparison>> getMarketVsMartComparisons(
            @Parameter(description = "전통시장명") @PathVariable String marketName) {
        
        log.info("전통시장 '{}' vs 대형마트 가격 비교 요청", marketName);

        try {
            List<RecommendationService.MarketVsMartComparison> comparisons = 
                recommendationService.getMarketVsMartComparisons(marketName);

            if (comparisons.isEmpty()) {
                return ApiResponse.success(
                    String.format("시장 '%s'와 대형마트 간 비교 가능한 아이템이 없거나 데이터가 부족합니다.", marketName), 
                    comparisons);
            }

            String message = String.format("시장 '%s'와 대형마트 간 가격 차이가 큰 아이템 %d개를 분석했습니다.", 
                                         marketName, comparisons.size());

            log.info("전통시장 '{}' vs 대형마트 비교 완료 - {} 건", marketName, comparisons.size());

            return ApiResponse.success(message, comparisons);

        } catch (Exception e) {
            log.error("전통시장 '{}' vs 대형마트 비교 중 오류 발생", marketName, e);
            return ApiResponse.failure("MARKET_VS_MART_COMPARISON_FAILED", 
                "전통시장 vs 대형마트 비교 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
