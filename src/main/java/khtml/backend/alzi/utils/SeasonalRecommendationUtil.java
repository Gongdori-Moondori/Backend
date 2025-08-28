package khtml.backend.alzi.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 계절별/월별 아이템 추천 유틸리티
 */
@Component
public class SeasonalRecommendationUtil {

    @Data
    public static class SeasonalRecommendation {
        private String itemName;
        private String category;
        private String reason;           // 추천 이유
        private double seasonalScore;    // 계절성 점수 (0-1)
        private String season;           // 계절명
        private List<String> benefits;   // 이 시기에 먹으면 좋은 이유들
    }

    // 월별 추천 아이템 매핑
    private static final Map<Month, List<SeasonalItem>> MONTHLY_RECOMMENDATIONS = new HashMap<>();

    @Data
    private static class SeasonalItem {
        private String name;
        private String category;
        private String reason;
        private double score;
        private List<String> benefits;

        public SeasonalItem(String name, String category, String reason, double score, List<String> benefits) {
            this.name = name;
            this.category = category;
            this.reason = reason;
            this.score = score;
            this.benefits = benefits;
        }
    }

    static {
        initializeSeasonalData();
    }

    /**
     * 계절별 추천 데이터 초기화
     */
    private static void initializeSeasonalData() {
        // 1월 - 겨울 제철 아이템
        MONTHLY_RECOMMENDATIONS.put(Month.JANUARY, Arrays.asList(
                new SeasonalItem("배추", "채소류", "김치 담그기 좋은 시기", 0.9,
                        Arrays.asList("비타민C 풍부", "면역력 강화", "김치 재료로 최적")),
                new SeasonalItem("무", "채소류", "겨울 무는 단맛이 좋음", 0.85,
                        Arrays.asList("소화 촉진", "비타민C", "국물 요리에 좋음")),
                new SeasonalItem("귤", "과일류", "겨울 대표 과일", 0.95,
                        Arrays.asList("비타민C", "감기 예방", "수분 보충"))
        ));

        // 2월 - 늦겨울
        MONTHLY_RECOMMENDATIONS.put(Month.FEBRUARY, Arrays.asList(
                new SeasonalItem("시금치", "채소류", "겨울 시금치는 당도가 높음", 0.9,
                        Arrays.asList("철분 풍부", "엽산", "면역력 강화")),
                new SeasonalItem("굴", "수산물", "굴의 제철", 0.95,
                        Arrays.asList("아연", "단백질", "타우린")),
                new SeasonalItem("딸기", "과일류", "딸기 시즌 시작", 0.8,
                        Arrays.asList("비타민C", "항산화", "비타민K"))
        ));

        // 3월 - 초봄
        MONTHLY_RECOMMENDATIONS.put(Month.MARCH, Arrays.asList(
                new SeasonalItem("냉이", "채소류", "봄나물의 대표", 0.95,
                        Arrays.asList("해독 작용", "비타민", "봄철 입맛 돋움")),
                new SeasonalItem("달래", "채소류", "봄의 전령", 0.9,
                        Arrays.asList("알리신", "살균 작용", "식욕 증진")),
                new SeasonalItem("딸기", "과일류", "딸기 제철", 0.95,
                        Arrays.asList("비타민C", "항산화", "면역력"))
        ));

        // 4월 - 봄
        MONTHLY_RECOMMENDATIONS.put(Month.APRIL, Arrays.asList(
                new SeasonalItem("봄동", "채소류", "봄동의 계절", 0.9,
                        Arrays.asList("비타민C", "식이섬유", "해독 작용")),
                new SeasonalItem("아스파라거스", "채소류", "봄철 별미", 0.85,
                        Arrays.asList("아스파르긴산", "피로 회복", "간 해독")),
                new SeasonalItem("주꾸미", "수산물", "봄 주꾸미", 0.9,
                        Arrays.asList("타우린", "단백질", "저칼로리"))
        ));

        // 5월 - 늦봄
        MONTHLY_RECOMMENDATIONS.put(Month.MAY, Arrays.asList(
                new SeasonalItem("상추", "채소류", "상추가 연하고 맛있는 시기", 0.85,
                        Arrays.asList("베타카로틴", "식이섬유", "저칼로리")),
                new SeasonalItem("참외", "과일류", "참외 시즌 시작", 0.8,
                        Arrays.asList("수분 보충", "칼륨", "비타민C")),
                new SeasonalItem("미나리", "채소류", "봄 미나리", 0.9,
                        Arrays.asList("해독 작용", "비타민", "간 건강"))
        ));

        // 6월 - 초여름
        MONTHLY_RECOMMENDATIONS.put(Month.JUNE, Arrays.asList(
                new SeasonalItem("오이", "채소류", "여름 준비, 수분 보충", 0.85,
                        Arrays.asList("수분 함량 높음", "칼륨", "냉각 효과")),
                new SeasonalItem("토마토", "채소류", "토마토 제철 시작", 0.9,
                        Arrays.asList("리코펜", "비타민C", "항산화")),
                new SeasonalItem("자두", "과일류", "초여름 과일", 0.8,
                        Arrays.asList("비타민A", "식이섬유", "피로 회복"))
        ));

        // 7월 - 여름
        MONTHLY_RECOMMENDATIONS.put(Month.JULY, Arrays.asList(
                new SeasonalItem("수박", "과일류", "여름 대표 과일", 0.95,
                        Arrays.asList("수분 보충", "시트룰린", "체온 조절")),
                new SeasonalItem("옥수수", "곡물류", "여름 옥수수", 0.9,
                        Arrays.asList("식이섬유", "비타민B", "포만감")),
                new SeasonalItem("가지", "채소류", "여름 가지", 0.85,
                        Arrays.asList("안토시아닌", "식이섬유", "저칼로리"))
        ));

        // 8월 - 한여름
        MONTHLY_RECOMMENDATIONS.put(Month.AUGUST, Arrays.asList(
                new SeasonalItem("복숭아", "과일류", "복숭아 제철", 0.95,
                        Arrays.asList("비타민C", "베타카로틴", "수분 보충")),
                new SeasonalItem("참외", "과일류", "참외 최성수기", 0.9,
                        Arrays.asList("수분", "칼륨", "더위 해소")),
                new SeasonalItem("호박", "채소류", "애호박 제철", 0.85,
                        Arrays.asList("베타카로틴", "비타민C", "식이섬유"))
        ));

        // 9월 - 초가을
        MONTHLY_RECOMMENDATIONS.put(Month.SEPTEMBER, Arrays.asList(
                new SeasonalItem("배", "과일류", "가을 배", 0.9,
                        Arrays.asList("수분", "식이섬유", "기관지에 좋음")),
                new SeasonalItem("포도", "과일류", "포도 제철", 0.95,
                        Arrays.asList("안토시아닌", "레스베라트롤", "항산화")),
                new SeasonalItem("고구마", "곡물류", "햇고구마", 0.85,
                        Arrays.asList("베타카로틴", "식이섬유", "포만감"))
        ));

        // 10월 - 가을
        MONTHLY_RECOMMENDATIONS.put(Month.OCTOBER, Arrays.asList(
                new SeasonalItem("감", "과일류", "감의 계절", 0.95,
                        Arrays.asList("비타민C", "베타카로틴", "타닌")),
                new SeasonalItem("밤", "견과류", "햇밤", 0.9,
                        Arrays.asList("탄수화물", "비타민B", "칼륨")),
                new SeasonalItem("사과", "과일류", "햇사과", 0.85,
                        Arrays.asList("식이섬유", "비타민C", "펙틴"))
        ));

        // 11월 - 늦가을
        MONTHLY_RECOMMENDATIONS.put(Month.NOVEMBER, Arrays.asList(
                new SeasonalItem("배추", "채소류", "김장배추 시기", 0.95,
                        Arrays.asList("비타민C", "식이섬유", "김치 재료")),
                new SeasonalItem("무", "채소류", "김장무", 0.9,
                        Arrays.asList("소화효소", "비타민C", "김치 재료")),
                new SeasonalItem("사과", "과일류", "사과 최성수기", 0.9,
                        Arrays.asList("펙틴", "비타민C", "항산화"))
        ));

        // 12월 - 초겨울
        MONTHLY_RECOMMENDATIONS.put(Month.DECEMBER, Arrays.asList(
                new SeasonalItem("대파", "채소류", "겨울 대파", 0.85,
                        Arrays.asList("알리신", "비타민K", "면역력")),
                new SeasonalItem("브로콜리", "채소류", "겨울 브로콜리", 0.9,
                        Arrays.asList("비타민C", "설포라판", "항암 효과")),
                new SeasonalItem("귤", "과일류", "겨울 귤", 0.95,
                        Arrays.asList("비타민C", "헤스페리딘", "감기 예방"))
        ));
    }

    /**
     * 현재 시기에 맞는 추천 아이템 조회 (3개)
     */
    public List<SeasonalRecommendation> getCurrentSeasonalRecommendations() {
        Month currentMonth = LocalDate.now().getMonth();
        String currentSeason = getSeason(currentMonth);

        List<SeasonalItem> monthlyItems = MONTHLY_RECOMMENDATIONS.getOrDefault(currentMonth, new ArrayList<>());

        return monthlyItems.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore())) // 점수 내림차순
                .limit(3)
                .map(item -> {
                    SeasonalRecommendation recommendation = new SeasonalRecommendation();
                    recommendation.setItemName(item.getName());
                    recommendation.setCategory(item.getCategory());
                    recommendation.setReason(item.getReason());
                    recommendation.setSeasonalScore(item.getScore());
                    recommendation.setSeason(currentSeason);
                    recommendation.setBenefits(item.getBenefits());
                    return recommendation;
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 월에 맞는 추천 아이템 조회
     */
    public List<SeasonalRecommendation> getSeasonalRecommendations(Month month) {
        String season = getSeason(month);
        List<SeasonalItem> monthlyItems = MONTHLY_RECOMMENDATIONS.getOrDefault(month, new ArrayList<>());

        return monthlyItems.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(3)
                .map(item -> {
                    SeasonalRecommendation recommendation = new SeasonalRecommendation();
                    recommendation.setItemName(item.getName());
                    recommendation.setCategory(item.getCategory());
                    recommendation.setReason(item.getReason());
                    recommendation.setSeasonalScore(item.getScore());
                    recommendation.setSeason(season);
                    recommendation.setBenefits(item.getBenefits());
                    return recommendation;
                })
                .collect(Collectors.toList());
    }

    /**
     * 월을 계절로 변환
     */
    private String getSeason(Month month) {
        return switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "겨울";
            case MARCH, APRIL, MAY -> "봄";
            case JUNE, JULY, AUGUST -> "여름";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "가을";
        };
    }

    /**
     * 모든 등록된 아이템명 조회 (다른 서비스에서 필터링용)
     */
    public Set<String> getAllSeasonalItemNames() {
        return MONTHLY_RECOMMENDATIONS.values().stream()
                .flatMap(List::stream)
                .map(SeasonalItem::getName)
                .collect(Collectors.toSet());
    }

    /**
     * 특정 아이템이 현재 제철인지 확인
     */
    public boolean isCurrentlyInSeason(String itemName) {
        Month currentMonth = LocalDate.now().getMonth();
        return MONTHLY_RECOMMENDATIONS.getOrDefault(currentMonth, new ArrayList<>())
                .stream()
                .anyMatch(item -> item.getName().equals(itemName));
    }

    /**
     * 아이템의 제철 월들 조회
     */
    public List<Month> getSeasonalMonths(String itemName) {
        return MONTHLY_RECOMMENDATIONS.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(item -> item.getName().equals(itemName)))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }
}
