package khtml.backend.alzi.favorite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.favorite.dto.AddFavoriteRequest;
import khtml.backend.alzi.favorite.dto.UpdateFavoritePriceRequest;
import khtml.backend.alzi.utils.ApiResponse;
import khtml.backend.alzi.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
@Tag(name = "Favorite API", description = "즐겨찾기 관리 API")
@Slf4j
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    
    @PostMapping
    @Operation(
        summary = "즐겨찾기 추가", 
        description = "아이템과 시장을 지정하여 즐겨찾기에 추가합니다. " +
                     "대형마트 가격과 비교하여 할인 정보도 함께 제공합니다."
    )
    public ResponseEntity<ApiResponse<FavoriteService.FavoriteItemResponse>> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request) {
        
        User user = SecurityUtils.getCurrentUser();
        log.info("즐겨찾기 추가 요청 - 사용자: {}, 아이템: {}, 시장: {}", 
                user.getUserId(), request.getItemName(), request.getMarketName());
        
        try {
            FavoriteService.FavoriteItemResponse response = favoriteService.addFavorite(
                    user, 
                    request.getItemName(),
                    request.getMarketName(),
                    request.getPrice(),
                    request.getPriceUnit(),
                    request.getMemo()
            );
            
            String message = String.format("'%s (%s)'를 즐겨찾기에 추가했습니다.", 
                    request.getItemName(), request.getMarketName());
            
            return ResponseEntity.ok(ApiResponse.success(message, response));
            
        } catch (Exception e) {
            log.error("즐겨찾기 추가 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("FAVORITE_ADD_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping
    @Operation(
        summary = "사용자 즐겨찾기 목록 조회", 
        description = "로그인한 사용자의 모든 즐겨찾기 목록을 조회합니다. " +
                     "총 즐겨찾기 개수, 총 절약 가능 금액, 평균 할인율 등의 통계도 함께 제공합니다."
    )
    public ResponseEntity<ApiResponse<FavoriteService.FavoriteListResponse>> getUserFavorites() {
        
        User user = SecurityUtils.getCurrentUser();
        log.info("즐겨찾기 목록 조회 - 사용자: {}", user.getUserId());
        
        try {
            FavoriteService.FavoriteListResponse response = favoriteService.getUserFavorites(user);
            
            String message = String.format("즐겨찾기 %d개를 조회했습니다. (총 절약 가능: %d원)", 
                    response.getTotalCount(), 
                    response.getTotalSavingsAmount().intValue());
            
            return ResponseEntity.ok(ApiResponse.success(message, response));
            
        } catch (Exception e) {
            log.error("즐겨찾기 목록 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("FAVORITES_FETCH_FAILED", e.getMessage()));
        }
    }
    
    @PutMapping("/{favoriteId}/price")
    @Operation(
        summary = "즐겨찾기 가격 업데이트", 
        description = "즐겨찾기의 가격을 업데이트합니다. 할인 정보도 함께 재계산됩니다."
    )
    public ResponseEntity<ApiResponse<FavoriteService.FavoriteItemResponse>> updateFavoritePrice(
            @Parameter(description = "즐겨찾기 ID") @PathVariable Long favoriteId,
            @Valid @RequestBody UpdateFavoritePriceRequest request) {
        
        User user = SecurityUtils.getCurrentUser();
        log.info("즐겨찾기 가격 업데이트 - 사용자: {}, 즐겨찾기 ID: {}, 새 가격: {}원", 
                user.getUserId(), favoriteId, request.getNewPrice());
        
        try {
            FavoriteService.FavoriteItemResponse response = favoriteService.updateFavoritePrice(
                    user, favoriteId, request.getNewPrice());
            
            String message = String.format("'%s (%s)' 가격이 %d원으로 업데이트되었습니다.", 
                    response.getItemName(), 
                    response.getMarketName(),
                    response.getFavoritePrice().intValue());
            
            return ResponseEntity.ok(ApiResponse.success(message, response));
            
        } catch (Exception e) {
            log.error("즐겨찾기 가격 업데이트 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("FAVORITE_PRICE_UPDATE_FAILED", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{favoriteId}")
    @Operation(
        summary = "즐겨찾기 삭제", 
        description = "특정 즐겨찾기를 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @Parameter(description = "즐겨찾기 ID") @PathVariable Long favoriteId) {
        
        User user = SecurityUtils.getCurrentUser();
        log.info("즐겨찾기 삭제 - 사용자: {}, 즐겨찾기 ID: {}", user.getUserId(), favoriteId);
        
        try {
            favoriteService.removeFavorite(user, favoriteId);
            
            return ResponseEntity.ok(ApiResponse.success("즐겨찾기가 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("즐겨찾기 삭제 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("FAVORITE_REMOVE_FAILED", e.getMessage()));
        }
    }
}
