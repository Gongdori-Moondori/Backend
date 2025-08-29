package khtml.backend.alzi.favorite;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.market.Market;
import khtml.backend.alzi.market.MarketRepository;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import khtml.backend.alzi.shopping.ItemRepository;
import khtml.backend.alzi.shopping.ItemPrice;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {
    
    private final FavoriteItemRepository favoriteItemRepository;
    private final ItemRepository itemRepository;
    private final MarketRepository marketRepository;
    private final ItemPriceRepository itemPriceRepository;
    
    // 대형마트 목록
    private static final List<String> LARGE_MART_NAMES = Arrays.asList(
        "이마트", "롯데마트", "홈플러스", "코스트코", "하나로마트"
    );
    
    /**
     * 즐겨찾기 추가
     */
    @Transactional
    public FavoriteItemResponse addFavorite(User user, String itemName, String marketName, 
                                          BigDecimal price, String priceUnit, String memo) {
        log.info("즐겨찾기 추가 요청 - 사용자: {}, 아이템: {}, 시장: {}, 가격: {}원", 
                user.getUserId(), itemName, marketName, price);
        
        try {
            // 1. 아이템 조회 (없으면 예외)
            Item item = itemRepository.findByName(itemName)
                    .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND, 
                            String.format("아이템 '%s'을 찾을 수 없습니다.", itemName)));
            
            // 2. 시장 조회 (없으면 예외)
            Market market = marketRepository.findByName(marketName)
                    .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND, 
                            String.format("시장 '%s'을 찾을 수 없습니다.", marketName)));
            
            // 3. 중복 체크
            if (favoriteItemRepository.existsByUserAndItemAndMarket(user, item, market)) {
                throw new CustomException(ErrorCode.DATA_INTEGRITY_VIOLATION, 
                        String.format("이미 즐겨찾기에 등록된 아이템입니다. (아이템: %s, 시장: %s)", itemName, marketName));
            }
            
            // 4. 즐겨찾기 생성 및 저장
            FavoriteItem favoriteItem = FavoriteItem.builder()
                    .user(user)
                    .item(item)
                    .market(market)
                    .favoritePrice(price)
                    .priceUnit(priceUnit)
                    .memo(memo)
                    .build();
            
            favoriteItemRepository.save(favoriteItem);
            
            log.info("즐겨찾기 추가 완료 - ID: {}", favoriteItem.getId());
            
            // 5. 응답 생성 (할인 정보 포함)
            return createFavoriteResponse(favoriteItem);
            
        } catch (CustomException e) {
            log.error("즐겨찾기 추가 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("즐겨찾기 추가 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "즐겨찾기 추가 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 즐겨찾기 삭제
     */
    @Transactional
    public void removeFavorite(User user, Long favoriteId) {
        log.info("즐겨찾기 삭제 요청 - 사용자: {}, 즐겨찾기 ID: {}", user.getUserId(), favoriteId);
        
        FavoriteItem favoriteItem = favoriteItemRepository.findById(favoriteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DATABASE_ERROR, "해당 즐겨찾기를 찾을 수 없습니다."));
        
        // 권한 확인
        if (!favoriteItem.getUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 즐겨찾기에 대한 권한이 없습니다.");
        }
        
        favoriteItemRepository.delete(favoriteItem);
        log.info("즐겨찾기 삭제 완료 - ID: {}", favoriteId);
    }
    
    /**
     * 사용자 즐겨찾기 목록 조회
     */
    @Transactional(readOnly = true)
    public FavoriteListResponse getUserFavorites(User user) {
        log.info("사용자 {} 즐겨찾기 목록 조회", user.getUserId());
        
        List<FavoriteItem> favoriteItems = favoriteItemRepository.findByUserOrderByCreatedAtDesc(user);
        
        List<FavoriteItemResponse> favoriteResponses = favoriteItems.stream()
                .map(this::createFavoriteResponse)
                .collect(Collectors.toList());
        
        // 통계 계산
        FavoriteStatistics statistics = calculateFavoriteStatistics(favoriteResponses);
        
        return FavoriteListResponse.builder()
                .favorites(favoriteResponses)
                .totalCount(statistics.getTotalCount())
                .totalSavingsAmount(statistics.getTotalSavingsAmount())
                .averageSavingsPercentage(statistics.getAverageSavingsPercentage())
                .topSavingItem(statistics.getTopSavingItem())
                .topSavingMarket(statistics.getTopSavingMarket())
                .build();
    }
    
    /**
     * 즐겨찾기 가격 업데이트
     */
    @Transactional
    public FavoriteItemResponse updateFavoritePrice(User user, Long favoriteId, BigDecimal newPrice) {
        log.info("즐겨찾기 가격 업데이트 - 사용자: {}, 즐겨찾기 ID: {}, 새 가격: {}원", 
                user.getUserId(), favoriteId, newPrice);
        
        FavoriteItem favoriteItem = favoriteItemRepository.findById(favoriteId)
                .orElseThrow(() -> new CustomException(ErrorCode.DATABASE_ERROR, "해당 즐겨찾기를 찾을 수 없습니다."));
        
        // 권한 확인
        if (!favoriteItem.getUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 즐겨찾기에 대한 권한이 없습니다.");
        }
        
        favoriteItem.updatePrice(newPrice);
        favoriteItemRepository.save(favoriteItem);
        
        log.info("즐겨찾기 가격 업데이트 완료 - ID: {}", favoriteId);
        return createFavoriteResponse(favoriteItem);
    }
    
    /**
     * 즐겨찾기 응답 객체 생성 (할인 정보 포함)
     */
    private FavoriteItemResponse createFavoriteResponse(FavoriteItem favoriteItem) {
        // 대형마트 평균 가격 계산
        BigDecimal largeMartAveragePrice = calculateLargeMartAveragePrice(favoriteItem.getItem().getName());
        
        // 할인 금액 및 비율 계산
        BigDecimal discountAmount = BigDecimal.ZERO;
        double discountPercentage = 0.0;
        
        if (largeMartAveragePrice.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = largeMartAveragePrice.subtract(favoriteItem.getFavoritePrice());
            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                discountPercentage = discountAmount
                        .divide(largeMartAveragePrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }
        
        return FavoriteItemResponse.builder()
                .id(favoriteItem.getId())
                .itemName(favoriteItem.getItem().getName())
                .itemCategory(favoriteItem.getItem().getCategory())
                .marketName(favoriteItem.getMarket().getName())
                .marketType(LARGE_MART_NAMES.contains(favoriteItem.getMarket().getName()) ? "LARGE_MART" : "TRADITIONAL")
                .favoritePrice(favoriteItem.getFavoritePrice())
                .priceUnit(favoriteItem.getPriceUnit())
                .largeMartAveragePrice(largeMartAveragePrice)
                .discountAmount(discountAmount)
                .discountPercentage(discountPercentage)
                .memo(favoriteItem.getMemo())
                .createdAt(favoriteItem.getCreatedAt())
                .updatedAt(favoriteItem.getUpdatedAt())
                .createdAtFormatted(favoriteItem.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .build();
    }
    
    /**
     * 대형마트 평균 가격 계산
     */
    private BigDecimal calculateLargeMartAveragePrice(String itemName) {
        List<ItemPrice> largeMartPrices = itemPriceRepository.findAllByItemName(itemName)
                .stream()
                .filter(ip -> LARGE_MART_NAMES.contains(ip.getMarket().getName()))
                .filter(ip -> ip.getPrice() != null && ip.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        if (largeMartPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalPrice = largeMartPrices.stream()
                .map(ItemPrice::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalPrice.divide(BigDecimal.valueOf(largeMartPrices.size()), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 즐겨찾기 통계 계산
     */
    private FavoriteStatistics calculateFavoriteStatistics(List<FavoriteItemResponse> favorites) {
        if (favorites.isEmpty()) {
            return FavoriteStatistics.builder()
                    .totalCount(0L)
                    .totalSavingsAmount(BigDecimal.ZERO)
                    .averageSavingsPercentage(0.0)
                    .build();
        }
        
        long totalCount = favorites.size();
        
        // 총 절약 금액 계산
        BigDecimal totalSavingsAmount = favorites.stream()
                .map(FavoriteItemResponse::getDiscountAmount)
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 평균 절약 비율 계산
        double averageSavingsPercentage = favorites.stream()
                .filter(fav -> fav.getDiscountPercentage() > 0)
                .mapToDouble(FavoriteItemResponse::getDiscountPercentage)
                .average()
                .orElse(0.0);
        
        // 가장 많이 절약하는 아이템
        Optional<FavoriteItemResponse> topSavingFavorite = favorites.stream()
                .filter(fav -> fav.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                .max((f1, f2) -> f1.getDiscountAmount().compareTo(f2.getDiscountAmount()));
        
        String topSavingItem = topSavingFavorite
                .map(fav -> fav.getItemName() + " (절약: " + fav.getDiscountAmount().intValue() + "원)")
                .orElse(null);
        
        // 가장 자주 이용하는 시장 (즐겨찾기 개수 기준)
        String topSavingMarket = favorites.stream()
                .collect(Collectors.groupingBy(FavoriteItemResponse::getMarketName, Collectors.counting()))
                .entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + "개)")
                .orElse(null);
        
        return FavoriteStatistics.builder()
                .totalCount(totalCount)
                .totalSavingsAmount(totalSavingsAmount)
                .averageSavingsPercentage(averageSavingsPercentage)
                .topSavingItem(topSavingItem)
                .topSavingMarket(topSavingMarket)
                .build();
    }
    
    // === DTO Classes ===
    
    @Data
    @lombok.Builder
    public static class FavoriteItemResponse {
        private Long id;
        private String itemName;
        private String itemCategory;
        private String marketName;
        private String marketType; // "LARGE_MART" or "TRADITIONAL"
        private BigDecimal favoritePrice; // 즐겨찾기 등록 가격
        private String priceUnit;
        private BigDecimal largeMartAveragePrice; // 대형마트 평균 가격
        private BigDecimal discountAmount; // 할인 금액
        private double discountPercentage; // 할인 비율 (%)
        private String memo;
        private LocalDateTime createdAt; // 추가일
        private LocalDateTime updatedAt;
        private String createdAtFormatted; // 포맷된 추가일
    }
    
    @Data
    @lombok.Builder
    public static class FavoriteListResponse {
        private List<FavoriteItemResponse> favorites;
        private Long totalCount; // 총 즐겨찾기 개수
        private BigDecimal totalSavingsAmount; // 총 절약 가능 금액
        private double averageSavingsPercentage; // 평균 절약 비율
        private String topSavingItem; // 가장 많이 절약하는 아이템
        private String topSavingMarket; // 가장 자주 이용하는 시장
    }
    
    @Data
    @lombok.Builder
    public static class FavoriteStatistics {
        private Long totalCount;
        private BigDecimal totalSavingsAmount;
        private double averageSavingsPercentage;
        private String topSavingItem;
        private String topSavingMarket;
    }
}
