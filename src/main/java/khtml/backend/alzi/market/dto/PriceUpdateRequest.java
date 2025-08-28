package khtml.backend.alzi.market.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceUpdateRequest {
    private String marketName;    // 시장명 (예: "하나로마트")
    private String itemName;      // 품목명 (예: "사과")  
    private String yearMonth;     // 조사년월 (예: "2025-08")
}
