package khtml.backend.alzi.batch.dto;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "ListNecessariesPricesService")
public class SeoulApiResponse {
    
    @JacksonXmlProperty(localName = "list_total_count")
    private int listTotalCount;
    
    @JacksonXmlProperty(localName = "RESULT")
    private Result result;
    
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "row")
    private List<PriceRow> rows;
    
    @Data
    public static class Result {
        @JacksonXmlProperty(localName = "CODE")
        private String code;
        
        @JacksonXmlProperty(localName = "MESSAGE")
        private String message;
    }
}
