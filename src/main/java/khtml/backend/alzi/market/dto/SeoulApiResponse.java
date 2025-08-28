package khtml.backend.alzi.market.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JacksonXmlRootElement(localName = "ListNecessariesPricesService")
public class SeoulApiResponse {
    
    @JacksonXmlProperty(localName = "list_total_count")
    private int listTotalCount;
    
    @JacksonXmlProperty(localName = "RESULT")
    private ApiResult result;
    
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "row")
    private List<PriceInfo> priceInfos;
    
    @Data
    @NoArgsConstructor
    public static class ApiResult {
        @JacksonXmlProperty(localName = "CODE")
        private String code;
        
        @JacksonXmlProperty(localName = "MESSAGE")
        private String message;
    }
    
    @Data
    @NoArgsConstructor
    public static class PriceInfo {
        @JacksonXmlProperty(localName = "P_SEQ")
        private Long priceSeq; // 가격일련번호
        
        @JacksonXmlProperty(localName = "P_YEAR_MONTH")
        private String yearMonth; // 조사년월 (예: 2025-08)
        
        @JacksonXmlProperty(localName = "M_SEQ")
        private Long marketSeq; // 시장일련번호
        
        @JacksonXmlProperty(localName = "M_NAME")
        private String marketName; // 시장명 (예: 경동시장)
        
        @JacksonXmlProperty(localName = "M_TYPE_CODE")
        private String marketTypeCode; // 시장유형코드 (001)
        
        @JacksonXmlProperty(localName = "M_TYPE_NAME")
        private String marketTypeName; // 시장유형명 (전통시장)
        
        @JacksonXmlProperty(localName = "M_GU_CODE")
        private String districtCode; // 구코드 (230000)
        
        @JacksonXmlProperty(localName = "M_GU_NAME")
        private String districtName; // 구이름 (동대문구)
        
        @JacksonXmlProperty(localName = "A_SEQ")
        private Long itemSeq; // 품목일련번호
        
        @JacksonXmlProperty(localName = "A_NAME")
        private String itemName; // 품목명 (예: 감자 100g)
        
        @JacksonXmlProperty(localName = "A_UNIT")
        private String itemUnit; // 단위 (예: 개)
        
        @JacksonXmlProperty(localName = "A_PRICE")
        private String price; // 가격 (예: 240)
        
        @JacksonXmlProperty(localName = "ADD_COL")
        private String additionalInfo; // 추가정보
        
        @JacksonXmlProperty(localName = "P_DATE")
        private String priceDate; // 가격조사일 (예: 2025-08-26)
    }
}
