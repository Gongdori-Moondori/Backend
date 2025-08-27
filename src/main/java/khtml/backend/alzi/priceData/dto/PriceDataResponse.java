package khtml.backend.alzi.priceData.dto;

import khtml.backend.alzi.priceData.PriceData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDataResponse {
    private String serialNumber;
    private String marketNumber;
    private String marketName;
    private String itemNumber;
    private String itemName;
    private String actualSalesSpecifications;
    private String price;
    private LocalDate date;
    private String note;
    private String marketTypeNumber;
    private String marketType;
    private String boroughCode;
    private String boroughName;
    
    public static PriceDataResponse from(PriceData priceData) {
        return PriceDataResponse.builder()
                .serialNumber(priceData.getSerialNumber())
                .marketNumber(priceData.getMarketNumber())
                .marketName(priceData.getMarketName())
                .itemNumber(priceData.getItemNuber()) // 오타가 있는 필드명 유지
                .itemName(priceData.getItemName())
                .actualSalesSpecifications(priceData.getActualSalesSpecifications())
                .price(priceData.getPrice())
                .date(priceData.getDate())
                .note(priceData.getNote())
                .marketTypeNumber(priceData.getMarketTypeNumber())
                .marketType(priceData.getMarketType())
                .boroughCode(priceData.getBoroughCode())
                .boroughName(priceData.getBoroughName())
                .build();
    }
    
    public static List<PriceDataResponse> fromList(List<PriceData> priceDataList) {
        return priceDataList.stream()
                .map(PriceDataResponse::from)
                .collect(Collectors.toList());
    }
}
