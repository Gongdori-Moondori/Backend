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
    
    @JacksonXmlElementWrapper(localName = "row")
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
        @JacksonXmlProperty(localName = "P_YEAR_MONTH")
        private String yearMonth; // 조사년월 (예: 202508)
        
        @JacksonXmlProperty(localName = "M_NAME")
        private String marketName; // 시장명 (예: 하나로마트)
        
        @JacksonXmlProperty(localName = "M_TYPE_CODE")
        private String marketTypeCode; // 시장유형코드 (03: 대형마트)
        
        @JacksonXmlProperty(localName = "M_TYPE_NAME")
        private String marketTypeName; // 시장유형명 (대형마트)
        
        @JacksonXmlProperty(localName = "M_SEQ")
        private String marketSeq; // 시장일련번호
        
        @JacksonXmlProperty(localName = "A_NAME")
        private String itemName; // 품목명 (예: 사과)
        
        @JacksonXmlProperty(localName = "A_UNIT")
        private String itemUnit; // 단위 (예: 개)
        
        @JacksonXmlProperty(localName = "A_PRICE")
        private String price; // 가격 (예: 3000)
        
        @JacksonXmlProperty(localName = "ADD_COL")
        private String additionalInfo; // 추가정보 (예: 5개10000)
        
        @JacksonXmlProperty(localName = "P_DATE")
        private String priceDate; // 가격조사일 (예: 20250815)
    }
}
