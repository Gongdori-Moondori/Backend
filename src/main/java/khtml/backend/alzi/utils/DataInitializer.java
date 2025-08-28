package khtml.backend.alzi.utils;

import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.market.MarketRepository;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import khtml.backend.alzi.shopping.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final MarketRepository marketRepository;
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final Random random = new Random();

    @Override
    public void run(ApplicationArguments args) {
        // 자동 실행하지 않음 - 수동 API 호출로만 실행
        // initializeData();
    }

    /**
     * 데이터 초기화 실행 (수동 호출용)
     */
    @Transactional
    public void initializeData() {
        log.info("데이터 초기화 시작");
        
        // 1. 대형마트 데이터 추가
        createLargeMarts();
        
        // 2. 대형마트 가격 데이터 생성
        createMartPriceData();
        
        log.info("데이터 초기화 완료");
    }

    /**
     * 대형마트 데이터 생성
     */
    private void createLargeMarts() {
        List<String> martNames = List.of("이마트", "롯데마트", "홈플러스");
        
        for (String martName : martNames) {
            if (!marketRepository.existsByName(martName)) {
                Market mart = Market.builder()
                    .code("MART_" + martName)
                    .name(martName)
                    .address(martName + " 본점")
                    .roadNameAddress(martName + " 본점 도로명주소")
                    .city("서울특별시")
                    .district("강남구")
                    .build();
                
                marketRepository.save(mart);
                log.info("대형마트 '{}' 데이터 생성 완료", martName);
            } else {
                log.info("대형마트 '{}' 이미 존재함", martName);
            }
        }
    }

    /**
     * 대형마트 가격 데이터 생성
     * 전통시장 가격보다 3,000원 ~ 15,000원 더 비싸게 설정
     */
    private void createMartPriceData() {
        List<String> martNames = List.of("이마트", "롯데마트", "홈플러스");
        List<Market> marts = marketRepository.findByNameIn(martNames);
        
        if (marts.isEmpty()) {
            log.warn("대형마트 데이터가 없어서 가격 데이터를 생성할 수 없습니다.");
            return;
        }

        // 모든 아이템 조회
        List<Item> allItems = itemRepository.findAll();
        log.info("총 {} 개 아이템에 대해 대형마트 가격 데이터 생성", allItems.size());

        int totalCreated = 0;
        
        for (Item item : allItems) {
            // 해당 아이템의 전통시장 평균 가격 계산
            BigDecimal marketAveragePrice = calculateTraditionalMarketAveragePrice(item);
            
            if (marketAveragePrice == null || marketAveragePrice.equals(BigDecimal.ZERO)) {
                // 전통시장 데이터가 없으면 기본 가격 설정
                marketAveragePrice = getDefaultItemPrice(item.getName());
            }

            for (Market mart : marts) {
                // 이미 해당 마트-아이템 조합의 가격이 있는지 확인
                if (itemPriceRepository.findByItemAndMarket(item, mart).isPresent()) {
                    continue; // 이미 있으면 건너뛰기
                }

                // 마트 가격 = 시장 가격 + (3,000 ~ 15,000원)
                int premiumAmount = 3000 + random.nextInt(12001); // 3000 ~ 15000
                BigDecimal martPrice = marketAveragePrice.add(BigDecimal.valueOf(premiumAmount));

                // 마트별로 약간의 가격 차이 추가 (±500원)
                int variation = random.nextInt(1001) - 500; // -500 ~ +500
                martPrice = martPrice.add(BigDecimal.valueOf(variation));
                
                // 최소 가격 보장 (1000원 이상)
                if (martPrice.compareTo(BigDecimal.valueOf(1000)) < 0) {
                    martPrice = BigDecimal.valueOf(1000);
                }

                ItemPrice martItemPrice = ItemPrice.builder()
                    .item(item)
                    .market(mart)
                    .price(martPrice)
                    .priceUnit(getDefaultPriceUnit(item.getCategory()))
                    .surveyDate(LocalDate.now())
                    .additionalInfo("초기 데이터 생성")
                    .build();

                itemPriceRepository.save(martItemPrice);
                totalCreated++;
            }
        }

        log.info("대형마트 가격 데이터 생성 완료 - 총 {} 건", totalCreated);
    }

    /**
     * 전통시장 평균 가격 계산
     */
    private BigDecimal calculateTraditionalMarketAveragePrice(Item item) {
        try {
            List<String> largeMarts = List.of("이마트", "롯데마트", "홈플러스");
            
            // 대형마트가 아닌 시장들의 가격만 조회
            List<ItemPrice> traditionalMarketPrices = itemPriceRepository.findByItem(item)
                .stream()
                .filter(ip -> ip.getMarket() != null && 
                             !largeMarts.contains(ip.getMarket().getName()))
                .toList();

            if (traditionalMarketPrices.isEmpty()) {
                return null;
            }

            BigDecimal sum = traditionalMarketPrices.stream()
                .map(ItemPrice::getPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return sum.divide(BigDecimal.valueOf(traditionalMarketPrices.size()), 
                             0, BigDecimal.ROUND_HALF_UP);

        } catch (Exception e) {
            log.debug("아이템 '{}' 전통시장 평균 가격 계산 실패: {}", item.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 기본 아이템 가격 설정 (전통시장 데이터가 없는 경우)
     */
    private BigDecimal getDefaultItemPrice(String itemName) {
        // 아이템별 기본 가격 설정
        return switch (itemName.toLowerCase()) {
            case "배추", "무" -> BigDecimal.valueOf(2000 + random.nextInt(1001)); // 2000~3000
            case "상추", "시금치", "냉이" -> BigDecimal.valueOf(1500 + random.nextInt(1001)); // 1500~2500
            case "토마토", "오이", "가지" -> BigDecimal.valueOf(2500 + random.nextInt(1001)); // 2500~3500
            case "수박" -> BigDecimal.valueOf(8000 + random.nextInt(2001)); // 8000~10000
            case "복숭아", "포도", "사과" -> BigDecimal.valueOf(4000 + random.nextInt(2001)); // 4000~6000
            case "딸기", "참외", "자두" -> BigDecimal.valueOf(3000 + random.nextInt(1501)); // 3000~4500
            case "쌀", "고구마" -> BigDecimal.valueOf(15000 + random.nextInt(5001)); // 15000~20000
            case "감", "밤", "귤" -> BigDecimal.valueOf(3500 + random.nextInt(1501)); // 3500~5000
            case "굴", "주꾸미" -> BigDecimal.valueOf(12000 + random.nextInt(3001)); // 12000~15000
            default -> BigDecimal.valueOf(3000 + random.nextInt(2001)); // 3000~5000 (기본)
        };
    }

    /**
     * 카테고리별 기본 단위 설정
     */
    private String getDefaultPriceUnit(String category) {
        if (category == null) return "1개";
        
        return switch (category.toLowerCase()) {
            case "채소류" -> "1kg";
            case "과일류" -> "1kg";
            case "곡물류" -> "1kg";
            case "수산물" -> "1kg";
            case "견과류" -> "1kg";
            default -> "1개";
        };
    }
}
