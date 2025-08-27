package khtml.backend.alzi.batch.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import khtml.backend.alzi.batch.dto.PriceRow;
import khtml.backend.alzi.batch.dto.SeoulApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeoulApiService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    
    @Value("${seoul.api.key:sample}")
    private String apiKey;
    
    @Value("${seoul.api.base-url:http://openAPI.seoul.go.kr:8088}")
    private String baseUrl;
    
    private static final int MAX_ROWS_PER_REQUEST = 1000;
    
    /**
     * 서울시 생필품 가격 정보 조회
     * @param marketName 시장명 (null이면 전체)
     * @param itemName 품목명 (null이면 전체)
     * @param yearMonth 년월 (YYYY-MM 형식)
     * @return 가격 정보 리스트
     */
    public List<PriceRow> getPriceData(String marketName, String itemName, String yearMonth) {
        try {
            // API URL 구성
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(apiKey, "xml", "ListNecessariesPricesService")
                .pathSegment("1", String.valueOf(MAX_ROWS_PER_REQUEST))
                .pathSegment(marketName != null ? marketName : "")
                .pathSegment(itemName != null ? itemName : "")
                .pathSegment(yearMonth != null ? yearMonth : "")
                .build()
                .toString();
            
            log.info("서울시 API 호출: {}", url);
            
            // API 호출
            String xmlResponse = restTemplate.getForObject(url, String.class);
            
            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                log.warn("API 응답이 비어있음 - 시장: {}, 품목: {}, 년월: {}", marketName, itemName, yearMonth);
                return List.of();
            }
            
            // XML 파싱
            SeoulApiResponse response = xmlMapper.readValue(xmlResponse, SeoulApiResponse.class);
            
            // 결과 확인
            if (response.getResult() != null && !"INFO-000".equals(response.getResult().getCode())) {
                log.warn("API 오류 응답 - 코드: {}, 메시지: {}", 
                    response.getResult().getCode(), response.getResult().getMessage());
                return List.of();
            }
            
            List<PriceRow> rows = response.getRows();
            if (rows != null && !rows.isEmpty()) {
                log.info("가격 정보 조회 성공 - 시장: {}, 품목: {}, 년월: {}, 건수: {}", 
                    marketName, itemName, yearMonth, rows.size());
                return rows;
            } else {
                log.info("가격 정보 없음 - 시장: {}, 품목: {}, 년월: {}", marketName, itemName, yearMonth);
                return List.of();
            }
            
        } catch (Exception e) {
            log.error("서울시 API 호출 중 오류 발생 - 시장: {}, 품목: {}, 년월: {}, 오류: {}", 
                marketName, itemName, yearMonth, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * 이전 달 년월 문자열 반환 (YYYY-MM 형식)
     */
    public String getPreviousMonthString() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        return previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    /**
     * 특정 품목의 이전 달 가격 정보 조회
     */
    public List<PriceRow> getPreviousMonthPriceData(String itemName) {
        String previousMonth = getPreviousMonthString();
        return getPriceData(null, itemName, previousMonth);
    }
}
