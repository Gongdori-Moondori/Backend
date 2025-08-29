package khtml.backend.alzi.shopping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.shopping.dto.AddItemToCartRequest;
import khtml.backend.alzi.shopping.dto.CompleteItemsRequest;
import khtml.backend.alzi.shopping.dto.CreateShoppingListRequest;
import khtml.backend.alzi.shopping.dto.ShoppingListResponse;
import khtml.backend.alzi.utils.ApiResponse;
import khtml.backend.alzi.utils.ItemCategoryUtil;
import khtml.backend.alzi.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/shopping")
@Tag(name = "Shopping API", description = "쇼핑 리스트 관리 API")
@Slf4j
public class ShoppingController {
	private final ShoppingService shoppingService;
	private final ItemPriceRepository itemPriceRepository;

	@PostMapping("/lists")
	@Operation(summary = "장바구니 생성", description = "아이템 리스트로 새로운 장바구니를 생성합니다. 아이템이 이미 존재하면 연결하고, 없으면 새로 생성합니다.")
	public ResponseEntity<ApiResponse<ShoppingListResponse>> createShoppingList(
		@RequestBody CreateShoppingListRequest request) {

		User user = SecurityUtils.getCurrentUser();
		log.info("장바구니 생성 요청 - 사용자: {}, 아이템 수: {}",
			user.getUserId(), request.getItems() != null ? request.getItems().size() : 0);

		try {
			ShoppingListResponse response = shoppingService.createShoppingList(request, user);
			return ResponseEntity.ok(ApiResponse.success("장바구니가 성공적으로 생성되었습니다.", response));
		} catch (Exception e) {
			log.error("장바구니 생성 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("장바구니 생성 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/lists")
	@Operation(summary = "사용자 장바구니 목록 조회", description = "로그인한 사용자의 모든 장바구니 목록을 조회합니다.")
	public ResponseEntity<ApiResponse<List<ShoppingListResponse>>> getUserShoppingLists() {

		User user = SecurityUtils.getCurrentUser();
		log.info("사용자 장바구니 목록 조회 - 사용자: {}", user.getUserId());

		try {
			List<ShoppingListResponse> response = shoppingService.getUserShoppingLists(user);
			return ResponseEntity.ok(ApiResponse.success("장바구니 목록을 성공적으로 조회했습니다.", response));
		} catch (Exception e) {
			log.error("장바구니 목록 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("장바구니 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/lists/{shoppingListId}")
	@Operation(summary = "특정 장바구니 조회", description = "특정 장바구니의 상세 정보를 조회합니다.")
	public ResponseEntity<ApiResponse<ShoppingListResponse>> getShoppingList(
		@PathVariable Long shoppingListId) {

		User user = SecurityUtils.getCurrentUser();
		log.info("장바구니 상세 조회 - 사용자: {}, 장바구니 ID: {}", user.getUserId(), shoppingListId);

		try {
			ShoppingListResponse response = shoppingService.getShoppingList(shoppingListId, user);
			return ResponseEntity.ok(ApiResponse.success("장바구니 정보를 성공적으로 조회했습니다.", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			log.error("장바구니 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("장바구니 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PostMapping("/test")
	public ApiResponse<?> addItem() {
		shoppingService.test();
		return ApiResponse.success();
	}
	
	@PostMapping("/admin/update-categories")
	@Operation(summary = "아이템 카테고리 일괄 업데이트", 
	           description = "기존 아이템들의 카테고리를 자동으로 분류하여 업데이트합니다. 카테고리가 null이거나 '기타'인 아이템들이 대상입니다.")
	public ApiResponse<?> updateItemCategories() {
		log.info("아이템 카테고리 일괄 업데이트 요청");
		
		try {
			shoppingService.updateItemCategories();
			return ApiResponse.success("아이템 카테고리가 성공적으로 업데이트되었습니다.");
		} catch (Exception e) {
			log.error("카테고리 업데이트 중 오류 발생", e);
			return ApiResponse.failure("카테고리 업데이트 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
	
	@GetMapping("/categories")
	@Operation(summary = "모든 카테고리 목록 조회", description = "시스템에서 사용하는 모든 아이템 카테고리 목록을 조회합니다.")
	public ResponseEntity<ApiResponse<Set<String>>> getAllCategories() {
		Set<String> categories = ItemCategoryUtil.getAllCategories();
		return ResponseEntity.ok(ApiResponse.success("카테고리 목록을 성공적으로 조회했습니다.", categories));
	}
	
	@GetMapping("/categories/{category}/items")
	@Operation(summary = "카테고리별 아이템 목록 조회", description = "특정 카테고리에 속하는 모든 아이템을 조회합니다.")
	public ResponseEntity<ApiResponse<Set<String>>> getItemsByCategory(@PathVariable String category) {
		Set<String> items = ItemCategoryUtil.getItemsByCategory(category);
		if (items.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ApiResponse.success(
			category + " 카테고리의 아이템 목록을 조회했습니다.", items));
	}

	@PatchMapping("/lists/{shoppingListId}/items/complete")
	@Operation(
		summary = "장바구니 아이템 구매 완료 처리", 
		description = "특정 장바구니에서 선택한 아이템들을 구매 완료 상태로 변경합니다. " +
					 "아이템 ID 리스트를 받아서 해당 아이템들의 상태를 PURCHASED로 업데이트합니다."
	)
	public ResponseEntity<ApiResponse<ShoppingListResponse>> completeShoppingItems(
		@Parameter(description = "장바구니 ID") @PathVariable Long shoppingListId,
		@Parameter(description = "완료 처리할 아이템 ID 리스트") @RequestBody CompleteItemsRequest request) {

		User user = SecurityUtils.getCurrentUser();
		log.info("장바구니 {} 아이템 구매 완료 처리 - 사용자: {}, 아이템 수: {}", 
			shoppingListId, user.getUserId(), request.getItemIds().size());

		try {
			ShoppingListResponse response = shoppingService.completeShoppingItems(
				shoppingListId, request.getItemIds(), user, request.getItemMarkets());
			return ResponseEntity.ok(ApiResponse.success("선택한 아이템들이 구매 완료로 처리되었습니다.", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("INVALID_REQUEST", e.getMessage()));
		} catch (Exception e) {
			log.error("아이템 구매 완료 처리 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("COMPLETE_ITEMS_FAILED", "아이템 구매 완료 처리 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PatchMapping("/lists/{shoppingListId}/items/cancel")
	@Operation(
		summary = "장바구니 아이템 취소 처리", 
		description = "특정 장바구니에서 선택한 아이템들을 취소 상태로 변경합니다."
	)
	public ResponseEntity<ApiResponse<ShoppingListResponse>> cancelShoppingItems(
		@Parameter(description = "장바구니 ID") @PathVariable Long shoppingListId,
		@Parameter(description = "취소 처리할 아이템 ID 리스트") @RequestBody CompleteItemsRequest request) {

		User user = SecurityUtils.getCurrentUser();
		log.info("장바구니 {} 아이템 취소 처리 - 사용자: {}, 아이템 수: {}", 
			shoppingListId, user.getUserId(), request.getItemIds().size());

		try {
			ShoppingListResponse response = shoppingService.cancelShoppingItems(shoppingListId, request.getItemIds(), user);
			return ResponseEntity.ok(ApiResponse.success("선택한 아이템들이 취소 처리되었습니다.", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("INVALID_REQUEST", e.getMessage()));
		} catch (Exception e) {
			log.error("아이템 취소 처리 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("CANCEL_ITEMS_FAILED", "아이템 취소 처리 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/lists/{shoppingListId}/statistics")
	@Operation(
		summary = "장바구니 통계 조회", 
		description = "특정 장바구니의 완료/미완료 아이템 통계를 조회합니다."
	)
	public ResponseEntity<ApiResponse<ShoppingService.ShoppingListStatistics>> getShoppingListStatistics(
		@Parameter(description = "장바구니 ID") @PathVariable Long shoppingListId) {

		User user = SecurityUtils.getCurrentUser();
		log.info("장바구니 {} 통계 조회 - 사용자: {}", shoppingListId, user.getUserId());

		try {
			ShoppingService.ShoppingListStatistics statistics = shoppingService.getShoppingListStatistics(shoppingListId, user);
			return ResponseEntity.ok(ApiResponse.success("장바구니 통계를 조회했습니다.", statistics));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("INVALID_REQUEST", e.getMessage()));
		} catch (Exception e) {
			log.error("장바구니 통계 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("STATISTICS_FAILED", "장바구니 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/frequent-items")
	@Operation(
		summary = "자주 구매한 상품 조회", 
		description = "사용자가 자주 구매한 상품 5개를 조회합니다. " +
					 "구매 횟수, 총 구매량, 평균 가격, 마지막 구매일 등의 정보를 제공합니다."
	)
	public ResponseEntity<ApiResponse<List<ShoppingService.FrequentItemResponse>>> getFrequentItems() {

		User user = SecurityUtils.getCurrentUser();
		log.info("자주 구매한 상품 조회 - 사용자: {}", user.getUserId());

		try {
			List<ShoppingService.FrequentItemResponse> frequentItems = shoppingService.getFrequentItems(user);
			
			if (frequentItems.isEmpty()) {
				return ResponseEntity.ok(ApiResponse.success("구매 기록이 없습니다.", frequentItems));
			}

			String message = String.format("자주 구매한 상품 %d개를 조회했습니다.", frequentItems.size());
			return ResponseEntity.ok(ApiResponse.success(message, frequentItems));

		} catch (Exception e) {
			log.error("자주 구매한 상품 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("FREQUENT_ITEMS_FAILED", "자주 구매한 상품 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/savings/statistics")
	@Operation(
		summary = "사용자 절약 통계 조회", 
		description = "사용자의 총 절약 금액, 절약 횟수, 아이템별/시장별 절약 현황 등을 조회합니다."
	)
	public ResponseEntity<ApiResponse<SavingsService.UserSavingsStats>> getSavingsStatistics() {

		User user = SecurityUtils.getCurrentUser();
		log.info("사용자 {} 절약 통계 조회", user.getUserId());

		try {
			SavingsService.UserSavingsStats stats = shoppingService.getSavingsStatistics(user);
			
			String message = String.format("총 %d회 구매로 %d원 절약하셨습니다!", 
				stats.getTotalSavingsCount(), stats.getTotalSavings().intValue());
			
			return ResponseEntity.ok(ApiResponse.success(message, stats));

		} catch (Exception e) {
			log.error("절약 통계 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("SAVINGS_STATS_FAILED", "절약 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PostMapping("/add-item")
	@Operation(
		summary = "현재 장바구니에 아이템 추가", 
		description = "현재 열려있는 장바구니에 아이템을 추가합니다. " +
					 "열린 장바구니가 없으면 새로 생성하고, 이미 있는 아이템이면 수량을 추가합니다."
	)
	public ResponseEntity<ApiResponse<ShoppingService.AddItemToCartResponse>> addItemToCurrentCart(
			@Valid @RequestBody AddItemToCartRequest request) {

		User user = SecurityUtils.getCurrentUser();
		log.info("현재 장바구니에 아이템 추가 - 사용자: {}, 아이템: {}, 수량: {}", 
			user.getUserId(), request.getItemName(), request.getQuantity());

		try {
			ShoppingService.AddItemToCartResponse response = shoppingService.addItemToCurrentCart(
				user, 
				request.getItemName(),
				request.getQuantity(),
				request.getCategory(),
				request.getMemo()
			);

			if (response.isSuccess()) {
				return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
			} else {
				return ResponseEntity.badRequest()
					.body(ApiResponse.failure("ADD_ITEM_FAILED", response.getMessage()));
			}

		} catch (Exception e) {
			log.error("장바구니 아이템 추가 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("ADD_ITEM_FAILED", "아이템 추가 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@GetMapping("/current")
	@Operation(
		summary = "현재 열린 장바구니 조회", 
		description = "현재 열려있는 장바구니 정보를 조회합니다. (PLANNED 또는 IN_PROGRESS 상태)"
	)
	public ResponseEntity<ApiResponse<ShoppingListResponse>> getCurrentShoppingList() {

		User user = SecurityUtils.getCurrentUser();
		log.info("현재 열린 장바구니 조회 - 사용자: {}", user.getUserId());

		try {
			Optional<ShoppingList> currentCartOpt = shoppingService.getCurrentOpenShoppingList(user);
			
			if (currentCartOpt.isPresent()) {
				ShoppingListResponse response = ShoppingListResponse.from(currentCartOpt.get());
				return ResponseEntity.ok(ApiResponse.success("현재 열린 장바구니를 조회했습니다.", response));
			} else {
				return ResponseEntity.ok(ApiResponse.success("현재 열린 장바구니가 없습니다.", null));
			}

		} catch (Exception e) {
			log.error("현재 장바구니 조회 중 오류 발생", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("CURRENT_CART_FAILED", "현재 장바구니 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}
}
