package khtml.backend.alzi.batch.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class PriceRow {
    
    @JacksonXmlProperty(localName = "P_SEQ")
    private Long pSeq;
    
    @JacksonXmlProperty(localName = "M_SEQ")
    private Long mSeq;
    
    @JacksonXmlProperty(localName = "M_NAME")
    private String mName; // 시장명
    
    @JacksonXmlProperty(localName = "A_SEQ")
    private Long aSeq;
    
    @JacksonXmlProperty(localName = "A_NAME")
    private String aName; // 품목명
    
    @JacksonXmlProperty(localName = "A_UNIT")
    private String aUnit; // 단위
    
    @JacksonXmlProperty(localName = "A_PRICE")
    private Integer aPrice; // 가격
    
    @JacksonXmlProperty(localName = "P_YEAR_MONTH")
    private String pYearMonth; // 년월
    
    @JacksonXmlProperty(localName = "ADD_COL")
    private String addCol; // 추가정보
    
    @JacksonXmlProperty(localName = "P_DATE")
    private String pDate; // 조사일자
    
    @JacksonXmlProperty(localName = "M_TYPE_CODE")
    private String mTypeCode; // 시장유형코드
    
    @JacksonXmlProperty(localName = "M_TYPE_NAME")
    private String mTypeName; // 시장유형명
    
    @JacksonXmlProperty(localName = "M_GU_CODE")
    private String mGuCode; // 구코드
    
    @JacksonXmlProperty(localName = "M_GU_NAME")
    private String mGuName; // 구명
}
