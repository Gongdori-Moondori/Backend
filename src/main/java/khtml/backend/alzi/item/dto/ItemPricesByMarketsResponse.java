package khtml.backend.alzi.item.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemPricesByMarketsResponse {
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private int totalMarkets; // 가격 정보가 있는 시장 수
    private BigDecimal averagePrice; // 평균 가격
    private BigDecimal minPrice; // 최저 가격
    private BigDecimal maxPrice; // 최고 가격
    private List<ItemPriceByMarketResponse> pricesByMarkets; // 시장별 가격 정보
    
    public static ItemPricesByMarketsResponse from(
            Long itemId, 
            String itemName, 
            String itemCategory,
            List<ItemPriceByMarketResponse> pricesByMarkets) {
        
        // 가격 통계 계산
        BigDecimal minPrice = pricesByMarkets.stream()
                .map(ItemPriceByMarketResponse::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
                
        BigDecimal maxPrice = pricesByMarkets.stream()
                .map(ItemPriceByMarketResponse::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
                
        BigDecimal averagePrice = pricesByMarkets.isEmpty() ? BigDecimal.ZERO :
                pricesByMarkets.stream()
                .map(ItemPriceByMarketResponse::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(pricesByMarkets.size()), 2, BigDecimal.ROUND_HALF_UP);
        
        return ItemPricesByMarketsResponse.builder()
                .itemId(itemId)
                .itemName(itemName)
                .itemCategory(itemCategory)
                .totalMarkets(pricesByMarkets.size())
                .averagePrice(averagePrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .pricesByMarkets(pricesByMarkets)
                .build();
    }
}
