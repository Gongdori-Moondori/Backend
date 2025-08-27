package khtml.backend.alzi.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import khtml.backend.alzi.batch.dto.PriceRow;
import khtml.backend.alzi.batch.service.SeoulApiService;
import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.market.MarketRepository;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceDataProcessor implements ItemProcessor<Item, List<ItemPrice>> {
    
    private final SeoulApiService seoulApiService;
    private final MarketRepository marketRepository;
    private final ItemPriceRepository itemPriceRepository;
    
    @Override
    public List<ItemPrice> process(Item item) throws Exception {
        log.info("가격 정보 처리 시작 - 품목: {}", item.getName());
        
        // 서울시 API에서 가격 정보 조회
        List<PriceRow> priceRows = seoulApiService.getPreviousMonthPriceData(item.getName());
        
        if (priceRows.isEmpty()) {
            log.info("가격 정보 없음 - 품목: {}", item.getName());
            return List.of();
        }
        
        // 각 시장별 가격 정보를 ItemPrice로 변환
        List<ItemPrice> itemPrices = priceRows.stream()
            .filter(row -> row.getAPrice() != null && row.getAPrice() > 0)
            .map(row -> processMarketPrice(item, row))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        log.info("가격 정보 처리 완료 - 품목: {}, 시장별 가격 수: {}", item.getName(), itemPrices.size());
        return itemPrices;
    }
    
    private Optional<ItemPrice> processMarketPrice(Item item, PriceRow row) {
        try {
            // 시장 정보 찾기 또는 생성
            Market market = findOrCreateMarket(row);
            if (market == null) {
                return Optional.empty();
            }
            
            // 기존 가격 정보 확인
            Optional<ItemPrice> existingPrice = itemPriceRepository.findByItemAndMarket(item, market);
            
            BigDecimal price = BigDecimal.valueOf(row.getAPrice());
            LocalDate surveyDate = parseDate(row.getPDate());
            
            if (existingPrice.isPresent()) {
                // 기존 가격 정보 업데이트
                ItemPrice itemPrice = existingPrice.get();
                itemPrice.updatePrice(price, row.getAUnit(), surveyDate, row.getAddCol());
                log.debug("기존 가격 정보 업데이트 - 아이템: {}, 시장: {}, 가격: {}", 
                    item.getName(), market.getName(), price);
                return Optional.of(itemPrice);
            } else {
                // 새로운 가격 정보 생성
                ItemPrice itemPrice = ItemPrice.builder()
                    .item(item)
                    .market(market)
                    .price(price)
                    .priceUnit(row.getAUnit())
                    .surveyDate(surveyDate)
                    .additionalInfo(row.getAddCol())
                    .build();
                
                log.debug("새 가격 정보 생성 - 아이템: {}, 시장: {}, 가격: {}", 
                    item.getName(), market.getName(), price);
                return Optional.of(itemPrice);
            }
        } catch (Exception e) {
            log.error("시장별 가격 처리 중 오류 - 아이템: {}, 시장: {}, 오류: {}", 
                item.getName(), row.getMName(), e.getMessage());
            return Optional.empty();
        }
    }
    
    private Market findOrCreateMarket(PriceRow row) {
        try {
            Optional<Market> existingMarket = marketRepository.findByName(row.getMName());
            
            if (existingMarket.isPresent()) {
                return existingMarket.get();
            } else {
                // 새 시장 생성 - code 자동 생성
                String marketCode = generateMarketCode(row.getMName(), row.getMGuName());
                
                Market newMarket = Market.builder()
                    .code(marketCode)
                    .name(row.getMName())
                    .type(row.getMTypeName())
                    .district(row.getMGuName())
                    .build();
                
                Market savedMarket = marketRepository.save(newMarket);
                log.info("새 시장 생성 - 코드: {}, 이름: {}, 타입: {}, 구: {}", 
                    marketCode, row.getMName(), row.getMTypeName(), row.getMGuName());
                return savedMarket;
            }
        } catch (Exception e) {
            log.error("시장 정보 처리 중 오류 - 시장명: {}, 오류: {}", row.getMName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 시장명과 구명을 기반으로 고유한 시장 코드 생성
     */
    private String generateMarketCode(String marketName, String district) {
        String baseCode = marketName.replaceAll("[^가-힣a-zA-Z0-9]", "");
        String districtCode = district != null ? district.replaceAll("[^가-힣a-zA-Z0-9]", "") : "";
        
        // 기본 코드 생성 (시장명 + 구명 + 타임스탬프)
        String code = baseCode + "_" + districtCode + "_" + System.currentTimeMillis();
        
        // 최대 길이 제한 (예: 50자)
        if (code.length() > 50) {
            code = code.substring(0, 50);
        }
        
        return code;
    }
    
    /**
     * 날짜 문자열을 LocalDate로 변환
     */
    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return LocalDate.now();
        }
        
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 현재 날짜로 대체", dateString);
            return LocalDate.now();
        }
    }
}
