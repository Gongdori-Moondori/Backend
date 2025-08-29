package khtml.backend.alzi.item.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemPriceByMarketResponse {
    private String marketCode;
    private String marketName;
    private String marketAddress;
    private String marketType;
    private BigDecimal price;
    private String priceUnit;
    private LocalDate surveyDate;
    private String additionalInfo;
    
    public static ItemPriceByMarketResponse from(khtml.backend.alzi.shopping.ItemPrice itemPrice) {
        return ItemPriceByMarketResponse.builder()
                .marketCode(itemPrice.getMarket().getCode())
                .marketName(itemPrice.getMarket().getName())
                .marketAddress(itemPrice.getMarket().getAddress())
                .marketType(itemPrice.getMarket().getType())
                .price(itemPrice.getPrice())
                .priceUnit(itemPrice.getPriceUnit())
                .surveyDate(itemPrice.getSurveyDate())
                .additionalInfo(itemPrice.getAdditionalInfo())
                .build();
    }
}
