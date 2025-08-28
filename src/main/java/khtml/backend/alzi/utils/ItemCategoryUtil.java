package khtml.backend.alzi.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 아이템 카테고리 자동 분류 유틸리티
 */
@Component
public class ItemCategoryUtil {
    
    // 카테고리별 아이템 매핑
    private static final Map<String, Set<String>> CATEGORY_ITEMS = new HashMap<>();
    
    // 카테고리 상수
    public static final String BEVERAGES = "음료";
    public static final String MEAT_SEAFOOD = "육류 및 수산물";
    public static final String GRAINS_NOODLES = "곡류 및 면류";
    public static final String VEGETABLES = "채소류";
    public static final String FRUITS = "과일류";
    public static final String PROCESSED_CONVENIENCE = "가공식품 및 간편식";
    public static final String SEASONINGS_SAUCES = "조미료 및 소스류";
    public static final String DAILY_NECESSITIES = "생활용품";
    public static final String OTHERS = "기타";
    
    static {
        // 음료
        CATEGORY_ITEMS.put(BEVERAGES, Set.of(
            "맥주", "사이다", "생수", "소주", "콜라", "우유"
        ));
        
        // 육류 및 수산물
        CATEGORY_ITEMS.put(MEAT_SEAFOOD, Set.of(
            "갈치", "갈치(생물)", "고등어", "고등어(신선냉장)", "굴", "꽃게(암게)", 
            "낙지", "닭고기", "돼지고기", "명태", "새우(흰다리새우)", "소고기(국산)", 
            "소고기(수입)", "소시지", "오징어", "오징어(국산", "전복", "조개(바지락)", 
            "조기", "조기(참조기)", "계란", "햄"
        ));
        
        // 곡류 및 면류
        CATEGORY_ITEMS.put(GRAINS_NOODLES, Set.of(
            "국수", "라면", "밀가루", "부침가루", "분유", "빵", "즉석밥", "컵라면", "콩"
        ));
        
        // 채소류
        CATEGORY_ITEMS.put(VEGETABLES, Set.of(
            "가지", "감자", "갓", "고구마", "고구마(밤고구마)", "당근", "대파", "도라지", 
            "무", "무(봄)", "미나리", "버섯(새송이버섯)", "부추", "브로콜리(국산)", 
            "상추", "상추(적상추)", "시금치", "애호박", "양배추", "양파", "열무", 
            "오이(다다기)", "쪽파", "콩나물", "토마토", "파프리카", "풋고추", 
            "깐마늘", "깻잎", "배추", "배추(여름)"
        ));
        
        // 과일류
        CATEGORY_ITEMS.put(FRUITS, Set.of(
            "귤", "귤(제주산)", "단감", "딸기", "바나나", "배(신고)", "복숭아(백도)", 
            "사과(부사)", "수박", "오렌지", "오렌지(수입산)", "참외", "포도(샤인머스켓)"
        ));
        
        // 가공식품 및 간편식
        CATEGORY_ITEMS.put(PROCESSED_CONVENIENCE, Set.of(
            "두부", "만두", "맛김", "어묵", "치즈", "통조림(참치)"
        ));
        
        // 조미료 및 소스류
        CATEGORY_ITEMS.put(SEASONINGS_SAUCES, Set.of(
            "간장", "고추장", "고춧가루(국산)", "굵은소금(천일염)", "된장", "마른멸치", 
            "마요네즈", "멸치액젓", "새우젓", "생강", "설탕", "식용유", "식초", 
            "참기름", "케찹"
        ));
        
        // 생활용품
        CATEGORY_ITEMS.put(DAILY_NECESSITIES, Set.of(
            "바디워시", "비누", "샴푸", "세제", "치약", "칫솔"
        ));
    }
    
    /**
     * 아이템명을 기반으로 카테고리를 자동으로 결정합니다.
     * 
     * @param itemName 아이템명
     * @return 해당하는 카테고리명, 찾을 수 없으면 "기타"
     */
    public String categorizeItem(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return OTHERS;
        }
        
        String cleanItemName = itemName.trim();
        
        // 정확히 일치하는 아이템명 찾기
        for (Map.Entry<String, Set<String>> entry : CATEGORY_ITEMS.entrySet()) {
            if (entry.getValue().contains(cleanItemName)) {
                return entry.getKey();
            }
        }
        
        // 부분 일치로 찾기 (괄호 안의 내용 제거 후)
        String baseItemName = cleanItemName.replaceAll("\\([^)]*\\)", "").trim();
        
        for (Map.Entry<String, Set<String>> entry : CATEGORY_ITEMS.entrySet()) {
            for (String categoryItem : entry.getValue()) {
                String baseCategoryItem = categoryItem.replaceAll("\\([^)]*\\)", "").trim();
                if (baseCategoryItem.equals(baseItemName)) {
                    return entry.getKey();
                }
            }
        }
        
        // 키워드 기반 매칭
        String lowerItemName = cleanItemName.toLowerCase();
        
        // 육류 키워드
        if (lowerItemName.contains("고기") || lowerItemName.contains("닭") || 
            lowerItemName.contains("돼지") || lowerItemName.contains("소")) {
            return MEAT_SEAFOOD;
        }
        
        // 수산물 키워드
        if (lowerItemName.contains("생선") || lowerItemName.contains("어") ||
            lowerItemName.contains("새우") || lowerItemName.contains("게") ||
            lowerItemName.contains("조개") || lowerItemName.contains("오징어")) {
            return MEAT_SEAFOOD;
        }
        
        // 과일 키워드
        if (lowerItemName.contains("과") || lowerItemName.endsWith("귤") ||
            lowerItemName.endsWith("감") || lowerItemName.contains("베리")) {
            return FRUITS;
        }
        
        // 채소 키워드
        if (lowerItemName.contains("채소") || lowerItemName.contains("나물") ||
            lowerItemName.endsWith("배추") || lowerItemName.endsWith("상추")) {
            return VEGETABLES;
        }
        
        // 음료 키워드
        if (lowerItemName.contains("음료") || lowerItemName.contains("주스") ||
            lowerItemName.contains("차") || lowerItemName.contains("물")) {
            return BEVERAGES;
        }
        
        return OTHERS;
    }
    
    /**
     * 모든 카테고리 목록을 반환합니다.
     */
    public static Set<String> getAllCategories() {
        return CATEGORY_ITEMS.keySet();
    }
    
    /**
     * 특정 카테고리의 모든 아이템을 반환합니다.
     */
    public static Set<String> getItemsByCategory(String category) {
        return CATEGORY_ITEMS.getOrDefault(category, Set.of());
    }
}
