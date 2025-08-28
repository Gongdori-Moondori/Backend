package khtml.backend.alzi.market;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import khtml.backend.alzi.market.dto.PriceUpdateRequest;
import khtml.backend.alzi.market.dto.SeoulApiResponse;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import khtml.backend.alzi.shopping.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeoulOpenApiService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    
    private final MarketRepository marketRepository;
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    
    // API KEY는 실제 환경에서는 application.properties에서 가져와야 합니다
    private static final String API_KEY = "sample"; // 실제 키로 변경 필요
    private static final String BASE_URL = "http://openAPI.seoul.go.kr:8088/{apiKey}/xml/ListNecessariesPricesService/{startIdx}/{endIdx}/{marketName}/{itemName}/{yearMonth}";
    
    /**
     * 서울 API에서 가격 정보를 가져와서 DB에 저장
     */
    @Transactional
    public void updatePricesFromSeoulApi(PriceUpdateRequest request) {
        try {
            log.info("서울 API에서 가격 정보 조회 시작: 시장={}, 품목={}, 년월={}", 
                    request.getMarketName(), request.getItemName(), request.getYearMonth());
            
            // API 호출
            String url = BASE_URL
                    .replace("{apiKey}", API_KEY)
                    .replace("{startIdx}", "1")
                    .replace("{endIdx}", "100")
                    .replace("{marketName}", request.getMarketName())
                    .replace("{itemName}", request.getItemName())
                    .replace("{yearMonth}", request.getYearMonth());
            
            String xmlResponse = restTemplate.getForObject(url, String.class);
            log.debug("API 응답: {}", xmlResponse);
            
            // XML 파싱
            SeoulApiResponse apiResponse = xmlMapper.readValue(xmlResponse, SeoulApiResponse.class);
            
            if (apiResponse.getResult().getCode().equals("INFO-000")) {
                log.info("API 호출 성공, {} 건의 가격 정보를 찾았습니다.", apiResponse.getListTotalCount());
                savePriceData(apiResponse.getPriceInfos());
            } else {
                log.warn("API 호출 실패: {}", apiResponse.getResult().getMessage());
            }
            
        } catch (Exception e) {
            log.error("서울 API에서 가격 정보 조회 중 오류 발생", e);
            throw new RuntimeException("가격 정보 조회 실패", e);
        }
    }
    
    /**
     * 모든 시장에 대해 특정 품목의 가격을 업데이트
     */
    @Transactional
    public void updateAllMarketPrices(String itemName, String yearMonth) {
        List<Market> markets = marketRepository.findAll();
        
        for (Market market : markets) {
            try {
                PriceUpdateRequest request = PriceUpdateRequest.builder()
                        .marketName(market.getName())
                        .itemName(itemName)
                        .yearMonth(yearMonth)
                        .build();
                
                updatePricesFromSeoulApi(request);
                
                // API 호출 간격 조절 (너무 빠른 요청 방지)
                Thread.sleep(100);
                
            } catch (Exception e) {
                log.warn("시장 '{}' 가격 정보 업데이트 실패: {}", market.getName(), e.getMessage());
            }
        }
    }
    
    /**
     * API 응답 데이터를 DB에 저장
     */
    private void savePriceData(List<SeoulApiResponse.PriceInfo> priceInfos) {
        if (priceInfos == null || priceInfos.isEmpty()) {
            log.warn("저장할 가격 데이터가 없습니다.");
            return;
        }
        
        for (SeoulApiResponse.PriceInfo priceInfo : priceInfos) {
            try {
                // 1. 시장 정보 찾기/생성
                Market market = findOrCreateMarket(priceInfo);
                
                // 2. 품목 정보 찾기/생성
                Item item = findOrCreateItem(priceInfo);
                
                // 3. 가격 정보 저장/업데이트
                saveOrUpdateItemPrice(priceInfo, market, item);
                
            } catch (Exception e) {
                log.error("가격 데이터 저장 중 오류 발생: {}", priceInfo, e);
            }
        }
    }
    
    private Market findOrCreateMarket(SeoulApiResponse.PriceInfo priceInfo) {
        // 시장 이름으로 검색
        Optional<Market> existingMarket = marketRepository.findByName(priceInfo.getMarketName());
        
        if (existingMarket.isPresent()) {
            return existingMarket.get();
        }
        
        // 새 시장 생성 (code는 시장명_일련번호 형태로 생성)
        String marketCode = priceInfo.getMarketName() + "_" + priceInfo.getMarketSeq();
        Market newMarket = Market.builder()
                .code(marketCode)
                .name(priceInfo.getMarketName())
                .type(priceInfo.getMarketTypeName())
                .build();
        
        return marketRepository.save(newMarket);
    }
    
    private Item findOrCreateItem(SeoulApiResponse.PriceInfo priceInfo) {
        Optional<Item> existingItem = itemRepository.findByName(priceInfo.getItemName());
        
        if (existingItem.isPresent()) {
            return existingItem.get();
        }
        
        // 새 품목 생성
        Item newItem = Item.builder()
                .name(priceInfo.getItemName())
                .category("API 수집") // 기본 카테고리
                .build();
        
        return itemRepository.save(newItem);
    }
    
    private void saveOrUpdateItemPrice(SeoulApiResponse.PriceInfo priceInfo, Market market, Item item) {
        try {
            // 가격 파싱
            BigDecimal price = new BigDecimal(priceInfo.getPrice());
            
            // 조사 날짜 파싱 (yyyyMMdd -> LocalDate)
            LocalDate surveyDate = LocalDate.parse(priceInfo.getPriceDate(), 
                    DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // 기존 가격 정보 찾기
            Optional<ItemPrice> existingPrice = itemPriceRepository
                    .findByItemAndMarketAndSurveyDate(item, market, surveyDate);
            
            if (existingPrice.isPresent()) {
                // 기존 데이터 업데이트
                ItemPrice itemPrice = existingPrice.get();
                itemPrice.updatePrice(price, priceInfo.getItemUnit(), surveyDate, priceInfo.getAdditionalInfo());
                itemPriceRepository.save(itemPrice);
                log.debug("가격 정보 업데이트: {} - {} - {}", item.getName(), market.getName(), price);
            } else {
                // 새 가격 정보 생성
                ItemPrice newItemPrice = ItemPrice.builder()
                        .item(item)
                        .market(market)
                        .price(price)
                        .priceUnit(priceInfo.getItemUnit())
                        .surveyDate(surveyDate)
                        .additionalInfo(priceInfo.getAdditionalInfo())
                        .build();
                
                itemPriceRepository.save(newItemPrice);
                log.debug("새 가격 정보 저장: {} - {} - {}", item.getName(), market.getName(), price);
            }
            
        } catch (Exception e) {
            log.error("가격 정보 저장 실패: {}", priceInfo, e);
        }
    }
}
