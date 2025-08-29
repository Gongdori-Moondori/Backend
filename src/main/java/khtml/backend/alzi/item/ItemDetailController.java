package khtml.backend.alzi.item;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.utils.ApiResponse;
import khtml.backend.alzi.item.dto.ItemPricesByMarketsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/item")
@Tag(name = "Item Detail API", description = "아이템 상세 정보 API")
@Slf4j
public class ItemDetailController {

    private final ItemDetailService itemDetailService;

    // @GetMapping("/{itemName}/detail")
    // @Operation(
    //     summary = "아이템 상세 정보 조회",
    //     description = "특정 아이템의 상세 정보를 제공합니다. " +
    //                  "아이템명, 카테고리, 현재 가격, 월별 가격 변화, 예측 가격 등을 포함합니다."
    // )
    // public ApiResponse<ItemDetailService.ItemDetailResponse> getItemDetail(
    //         @Parameter(description = "조회할 아이템명") @PathVariable String itemName) {
    //
    //     log.info("아이템 '{}' 상세 정보 조회 요청", itemName);
    //
    //     try {
    //         ItemDetailService.ItemDetailResponse itemDetail =
    //             itemDetailService.getItemDetail(itemName);
    //
    //         if (itemDetail == null) {
    //             return ApiResponse.failure("ITEM_NOT_FOUND",
    //                 String.format("아이템 '%s'에 대한 정보를 찾을 수 없습니다.", itemName));
    //         }
    //
    //         String message = String.format("아이템 '%s'의 상세 정보입니다.", itemName);
    //
    //         log.info("아이템 '{}' 상세 정보 조회 완료", itemName);
    //
    //         return ApiResponse.success(message, itemDetail);
    //
    //     } catch (Exception e) {
    //         log.error("아이템 '{}' 상세 정보 조회 중 오류 발생", itemName, e);
    //         return ApiResponse.failure("ITEM_DETAIL_FAILED",
    //             "아이템 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
    //     }
    // }

    @GetMapping("/{itemName}/market/{marketName}/detail")
    @Operation(
        summary = "특정 시장에서의 아이템 상세 정보 조회", 
        description = "특정 시장에서 판매되는 아이템의 상세 정보를 제공합니다. " +
                     "해당 시장에서의 가격 변화와 다른 시장과의 비교 정보를 포함합니다."
    )
    public ApiResponse<ItemDetailService.MarketItemDetailResponse> getMarketItemDetail(
            @Parameter(description = "조회할 아이템명") @PathVariable String itemName,
            @Parameter(description = "조회할 시장명") @PathVariable String marketName) {
        
        log.info("시장 '{}' 아이템 '{}' 상세 정보 조회 요청", marketName, itemName);

        try {
            ItemDetailService.MarketItemDetailResponse itemDetail = 
                itemDetailService.getMarketItemDetail(itemName, marketName);

            if (itemDetail == null) {
                return ApiResponse.failure("MARKET_ITEM_NOT_FOUND", 
                    String.format("시장 '%s'에서 아이템 '%s'에 대한 정보를 찾을 수 없습니다.", marketName, itemName));
            }

            String message = String.format("시장 '%s'에서 판매되는 아이템 '%s'의 상세 정보입니다.", marketName, itemName);

            log.info("시장 '{}' 아이템 '{}' 상세 정보 조회 완료", marketName, itemName);

            return ApiResponse.success(message, itemDetail);

        } catch (Exception e) {
            log.error("시장 '{}' 아이템 '{}' 상세 정보 조회 중 오류 발생", marketName, itemName, e);
            return ApiResponse.failure("MARKET_ITEM_DETAIL_FAILED", 
                "시장별 아이템 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{itemId}/prices")
    @Operation(
        summary = "아이템 ID로 시장별 가격 조회",
        description = "특정 아이템의 모든 시장에서의 가격 정보를 조회합니다. " +
                     "0원인 가격 정보는 제외됩니다. 가격순으로 정렬되어 반환됩니다."
    )
    public ApiResponse<ItemPricesByMarketsResponse> getItemPricesByMarkets(
            @Parameter(description = "조회할 아이템 ID") @PathVariable Long itemId) {
        
        log.info("아이템 ID {} 시장별 가격 조회 요청", itemId);

        try {
            ItemPricesByMarketsResponse response = itemDetailService.getItemPricesByMarkets(itemId);
            
            String message = String.format("아이템 '%s'의 시장별 가격 정보입니다. (총 %d개 시장)", 
                    response.getItemName(), response.getTotalMarkets());

            log.info("아이템 ID {} 시장별 가격 조회 완료 - {} 개 시장", itemId, response.getTotalMarkets());

            return ApiResponse.success(message, response);

        } catch (Exception e) {
            log.error("아이템 ID {} 시장별 가격 조회 중 오류 발생: {}", itemId, e.getMessage(), e);
            return ApiResponse.failure("ITEM_PRICES_FETCH_FAILED", 
                "시장별 가격 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/name/{itemName}/prices")
    @Operation(
        summary = "아이템명으로 시장별 가격 조회",
        description = "특정 아이템명의 모든 시장에서의 가격 정보를 조회합니다. " +
                     "0원인 가격 정보는 제외됩니다. 가격순으로 정렬되어 반환됩니다."
    )
    public ApiResponse<ItemPricesByMarketsResponse> getItemPricesByMarketsWithName(
            @Parameter(description = "조회할 아이템명") @PathVariable String itemName) {
        
        log.info("아이템명 '{}' 시장별 가격 조회 요청", itemName);

        try {
            ItemPricesByMarketsResponse response = itemDetailService.getItemPricesByMarketsWithName(itemName);
            
            String message = String.format("아이템 '%s'의 시장별 가격 정보입니다. (총 %d개 시장)", 
                    response.getItemName(), response.getTotalMarkets());

            log.info("아이템명 '{}' 시장별 가격 조회 완료 - {} 개 시장", itemName, response.getTotalMarkets());

            return ApiResponse.success(message, response);

        } catch (Exception e) {
            log.error("아이템명 '{}' 시장별 가격 조회 중 오류 발생: {}", itemName, e.getMessage(), e);
            return ApiResponse.failure("ITEM_PRICES_FETCH_FAILED", 
                "시장별 가격 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
