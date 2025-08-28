package khtml.backend.alzi.shopping;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.auth.user.User;
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
}
