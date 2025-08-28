package khtml.backend.alzi.market.dto.response;

import khtml.backend.alzi.priceData.PriceData;
import khtml.backend.alzi.shopping.ItemPrice;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MarketItemPriceResponse {
    private String marketName;
    private String itemName;
    private String category;
    
    // PriceData (과거 데이터)
    private List<PriceDataInfo> priceDataList;
    
    // ItemPrice (현재 시세 데이터)
    private List<ItemPriceInfo> itemPriceList;
    
    @Data
    @Builder
    public static class PriceDataInfo {
        private String itemName;
        private String marketName;
        private String price;
        private String unit;
        private String grade;
        private LocalDate date;
        private String source; // 데이터 출처
        
        // String price를 BigDecimal로 변환하는 유틸리티 메소드
        public BigDecimal getPriceAsBigDecimal() {
            if (price == null || price.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(price.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        
        public static PriceDataInfo from(PriceData priceData) {
            return PriceDataInfo.builder()
                    .itemName(priceData.getItemName())
                    .marketName(priceData.getMarketName())
                    .price(priceData.getPrice())
                    .unit(priceData.getActualSalesSpecifications()) // unit 대신 actualSalesSpecifications 사용
                    .grade(priceData.getNote()) // grade 대신 note 사용
                    .date(priceData.getDate())
                    .source("PriceData")
                    .build();
        }
    }
    
    @Data
    @Builder
    public static class ItemPriceInfo {
        private String itemName;
        private String marketName;
        private BigDecimal price;
        private String priceUnit;
        private LocalDate surveyDate;
        private String additionalInfo;
        private LocalDateTime createdAt;
        private String source; // 데이터 출처
        
        public static ItemPriceInfo from(ItemPrice itemPrice) {
            return ItemPriceInfo.builder()
                    .itemName(itemPrice.getItem().getName())
                    .marketName(itemPrice.getMarket().getName())
                    .price(itemPrice.getPrice())
                    .priceUnit(itemPrice.getPriceUnit())
                    .surveyDate(itemPrice.getSurveyDate())
                    .additionalInfo(itemPrice.getAdditionalInfo())
                    .createdAt(itemPrice.getCreatedAt())
                    .source("ItemPrice") // 구분을 위한 소스 표시
                    .build();
        }
    }
}
